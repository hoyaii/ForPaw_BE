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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public UserResponse.LoginTokenDTO login(UserRequest.LoginDTO requestDTO){

        User user = userRepository.findByEmail(requestDTO.email()).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_EMAIL_NOT_FOUND)
        );

        if(!passwordEncoder.matches(requestDTO.password(), user.getPassword())){
            throw new CustomException(ExceptionCode.USER_PASSWORD_WRONG);
        }

        String accessToken = JWTProvider.createAccessToken(user);
        String refreshToken = JWTProvider.createRefreshToken(user);

        // Refresh Token을 레디스에 저장
        redisService.storeToken("refreshToken", refreshToken, JWTProvider.REFRESH_EXP);

        return new UserResponse.LoginTokenDTO(accessToken, refreshToken);
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
        redisService.storeToken("emailCheckToken", token, 10 * 60 * 1000L); // 10분 유효

        return new UserResponse.EmailTokenDTO(token);
    }

    @Transactional
    public void sendRegisterCode(UserRequest.SendCodeDTO requestDTO){
        // 요청으로 온 토큰과 저장한 토큰을 비교해 검증해서, 임의적인 요청 방지
        if(!redisService.isTokenValid("emailCheckToken", requestDTO.validationToken()))
            throw new CustomException(ExceptionCode.BAD_APPROACH);
        redisService.removeToken("emailCheckToken", requestDTO.validationToken());

        // 인증 코드 전송 및 레디스에 저장
        String verificationCode = sendCodeByMail(requestDTO.email());
        redisService.storeVerificationCode(requestDTO.email(), verificationCode, 5 * 60 * 1000L); // 5분 동안 유효
    }

    @Transactional
    public void verifyRegisterCode(UserRequest.VerifyCodeDTO requestDTO){

        if(redisService.isVerificationCodeValid(requestDTO.email(), requestDTO.code()))
            throw new CustomException(ExceptionCode.CODE_WRONG);
        redisService.removeVerificationCode(requestDTO.email()); // 검증 후 토큰 삭제
    }

    @Transactional
    public void sendRecoveryCode(UserRequest.EmailDTO requestDTO){
        // 가입된 계정이 아니라면
        if(userRepository.findByEmail(requestDTO.email()).isEmpty())
            throw new CustomException(ExceptionCode.USER_EMAIL_NOT_FOUND);

        // 요청 횟수 3회 넘아가면 10분 동안 요청 불가
        Long recoveryNum = redisService.getRecoveryNum(requestDTO.email());
        if(recoveryNum >= 3L){
            throw new CustomException(ExceptionCode.EXCEED_REQUEST_NUM);
        }

        // 요청 횟수 업데이트
        redisService.storeRecoveryNum(requestDTO.email(), recoveryNum + 1, 10 * 60 * 1000L);

        // 인증 코드 전송 및 레디스에 저장
        String verificationCode = sendCodeByMail(requestDTO.email());
        redisService.storeVerificationCode(requestDTO.email(), verificationCode, 5 * 60 * 1000L);
    }

    @Transactional
    public void verifyRecoveryCode(UserRequest.VerifyCodeDTO requestDTO){

    }

    private String sendCodeByMail(String toEmail){
        String verificationCode = generateVerificationCode();
        String subject = "[ForPaw] 이메일 인증 코드입니다.";
        String text = "인증 코드는 다음과 같습니다: " + verificationCode + "\n이 코드를 입력하여 이메일을 인증해 주세요.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);

        return verificationCode;
    }

    private String generateVerificationCode() {

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();

        return IntStream.range(0, 8) // 8자리
                .map(i -> random.nextInt(chars.length()))
                .mapToObj(chars::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
    }
}