package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.UserRequest;
import com.hong.ForPaw.controller.DTO.UserResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.User.Role;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hong.ForPaw.core.security.JWTProvider;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final JavaMailSender mailSender;


    @Value("${spring.mail.username}")
    private String fromEmail;

    @Transactional
    public UserResponse.JwtTokenDTO login(UserRequest.LoginDTO requestDTO){

        User user = userRepository.findByEmail(requestDTO.email()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_ACCOUNT_WRONG)
        );

        if(!passwordEncoder.matches(requestDTO.password(), user.getPassword())){
            throw new CustomException(ExceptionCode.USER_ACCOUNT_WRONG);
        }

        String accessToken = JWTProvider.createAccessToken(user);
        String refreshToken = JWTProvider.createRefreshToken(user);

        // Refresh Token 갱신
        redisService.removeData("refreshToken", String.valueOf(user.getId()));
        redisService.storeDate("refreshToken", String.valueOf(user.getId()), refreshToken, JWTProvider.REFRESH_EXP);

        return new UserResponse.JwtTokenDTO(accessToken, refreshToken);
    }

    @Transactional
    public void join(UserRequest.JoinDTO requestDTO){

        if (!requestDTO.password().equals(requestDTO.passwordConfirm()))
            throw new CustomException(ExceptionCode.USER_PASSWORD_WRONG);

        User user = User.builder()
                .name(requestDTO.name())
                .nickName(requestDTO.nickName())
                .email(requestDTO.email())
                .password(passwordEncoder.encode(requestDTO.password()))
                .role(Role.USER)
                .profileURL(requestDTO.profileURL())
                .regin(requestDTO.region())
                .subRegion(requestDTO.subRegion())
                .build();

        userRepository.save(user);
    }

    @Transactional
    public UserResponse.EmailTokenDTO checkEmail(UserRequest.EmailDTO requestDTO){
        // 가입한 이메일이 존재 한다면
        if(userRepository.findByEmail(requestDTO.email()).isPresent())
            throw new CustomException(ExceptionCode.USER_EMAIL_EXIST);

        // 이메일 중복 체크를 하지 않고, 인위적으로 메일 전송 요청을 방지하기 위해, 토큰을 저장
        String token = UUID.randomUUID().toString();
        redisService.storeDate("emailToken", requestDTO.email(), token, 10 * 60 * 1000L); // 10분 유효

        return new UserResponse.EmailTokenDTO(token);
    }

    @Transactional
    public void sendRegisterCode(UserRequest.SendCodeDTO requestDTO){

        if(!redisService.validateData("emailToken", requestDTO.email(), requestDTO.validationToken()))
            throw new CustomException(ExceptionCode.BAD_APPROACH);
        redisService.removeData("emailToken", requestDTO.email());

        // 인증 코드 전송 및 레디스에 저장
        String verificationCode = sendCodeByMail(requestDTO.email());
        redisService.storeDate("emailCode", requestDTO.email(), verificationCode, 5 * 60 * 1000L); // 5분 동안 유효
    }

    @Transactional
    public void verifyCode(UserRequest.VerifyCodeDTO requestDTO){
        // 레디스를 통해 해당 코드가 유효한지 확인
        if(!redisService.validateData("emailCode", requestDTO.email(), requestDTO.code()))
            throw new CustomException(ExceptionCode.CODE_WRONG);
        redisService.removeData("emailCode", requestDTO.email()); // 검증 후 토큰 삭제
    }

    @Transactional
    public void sendRecoveryCode(UserRequest.EmailDTO requestDTO){
        // 가입된 계정이 아니라면
        if(userRepository.findByEmail(requestDTO.email()).isEmpty())
            throw new CustomException(ExceptionCode.USER_EMAIL_NOT_FOUND);

        // 요청 횟수 3회 넘아가면 10분 동안 요청 불가
        Long recoveryNum = redisService.getDataInLong("requestNum", requestDTO.email());
        if(recoveryNum >= 3L){
            throw new CustomException(ExceptionCode.EXCEED_REQUEST_NUM);
        }

        // 요청 횟수 업데이트
        redisService.storeDate("requestNum", requestDTO.email(), String.valueOf(recoveryNum + 1), 10 * 60 * 1000L);

        // 인증 코드 전송 및 레디스에 저장
        String verificationCode = sendCodeByMail(requestDTO.email());
        redisService.storeDate("emailCode", requestDTO.email(), verificationCode, 5 * 60 * 1000L);
    }

    @Transactional
    public void verifyAndSendPassword(UserRequest.VerifyCodeDTO requestDTO){

        // 레디스를 통해 해당 코드가 유효한지 확인
        if(!redisService.validateData("emailCode", requestDTO.email(), requestDTO.code()))
            throw new CustomException(ExceptionCode.CODE_WRONG);
        redisService.removeData("emailCode", requestDTO.email());

        // 임시 비밀번호 생성 후 업데이트
        String password = generateTemporaryPassword();
        User user = userRepository.findByEmail(requestDTO.email()).get(); // 이미 앞에서 계정 존재 여부를 확인 했으니 바로 가져옴
        user.updatePassword(passwordEncoder.encode(password));

        sendPasswordByMail(requestDTO.email(), password);
    }

    @Transactional
    public void changePassword(UserRequest.ChangePasswordDTO requestDTO, Long userId){

        User user = userRepository.findById(userId).get();

        if(!passwordEncoder.matches(requestDTO.curPassword(), user.getPassword())){
            throw new CustomException(ExceptionCode.USER_ACCOUNT_WRONG);
        }

        if (!requestDTO.newPassword().equals(requestDTO.newPasswordConfirm()))
            throw new CustomException(ExceptionCode.USER_PASSWORD_MATCH_WRONG);

        user.updatePassword(passwordEncoder.encode(requestDTO.newPassword()));
    }

    @Transactional
    public UserResponse.AccessTokenDTO updateAccessToken(UserRequest.RefreshTokenDTO requestDTO){
        // 잘못된 토큰 형식인지 체크
        if(!JWTProvider.validateToken(requestDTO.refreshToken())) {
            throw new CustomException(ExceptionCode.TOKEN_WRONG);
        }

        Long userId =JWTProvider.getUserIdFromToken(requestDTO.refreshToken());

        // 토큰 만료 여부 체크
        if(!redisService.isDateExist("refreshToken", String.valueOf(userId)))
            throw new CustomException(ExceptionCode.TOKEN_EXPIRED);

        // 탈퇴한 회원의 refreshToken이 요청으로 왔으면, USER_NOT_FOUND 에러 발생
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        return new UserResponse.AccessTokenDTO(JWTProvider.createAccessToken(user));
    }

    private String sendCodeByMail(String toEmail){

        String verificationCode = generateVerificationCode();
        String subject = "[ForPaw] 이메일 인증 코드입니다.";
        String text = "인증 코드는 다음과 같습니다: " + verificationCode + "\n이 코드를 입력하여 이메일을 인증해 주세요.";
        sendMail(fromEmail, toEmail, subject, text);

        return verificationCode;
    }

    private void sendPasswordByMail(String toEmail, String password){

        String subject = "[ForPaw] 임시 비밀번호 입니다.";
        String text = "임시 비밀번호: " + password + "\n로그인 후 비밀번호를 변경해 주세요.";
        sendMail(fromEmail, toEmail, subject, text);
    }

    private void sendMail(String fromEmail, String toEmail, String subject, String text){

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    // 알파벳, 숫자를 조합해서 인증 코드 생성
    private String generateVerificationCode() {

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();

        return IntStream.range(0, 8) // 8자리
                .map(i -> random.nextInt(chars.length()))
                .mapToObj(chars::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    // 알파벳, 숫자, 특수문자가 모두 포함되도록 해서 임시 비밀번호 생성
    private String generateTemporaryPassword() {

        String specialChars = "!@#$%^&*";
        String numbers = "0123456789";
        String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        SecureRandom random = new SecureRandom();

        // 각 범주에서 랜덤하게 하나씩 선택
        String mandatoryChars =
                Stream.of(specialChars, numbers, upperCaseLetters, lowerCaseLetters)
                        .map(s -> s.charAt(random.nextInt(s.length())))
                        .map(Object::toString)
                        .collect(Collectors.joining());

        // 나머지 자리를 전체 문자 집합에서 선택
        String allChars = specialChars + numbers + upperCaseLetters + lowerCaseLetters;
        String randomChars = IntStream.range(mandatoryChars.length(), 8)
                .map(i -> random.nextInt(allChars.length()))
                .mapToObj(allChars::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());

        // 최종 문자열 생성을 위해 섞기
        String verificationCode = Stream.of(mandatoryChars.split(""), randomChars.split(""))
                .flatMap(Arrays::stream)
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    Collections.shuffle(list);
                    return list.stream();
                }))
                .collect(Collectors.joining());

        return verificationCode;
    }
}