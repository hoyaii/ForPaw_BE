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
        redisService.storeToken("refreshToken:" + refreshToken, " ", JWTProvider.REFRESH_EXP);

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

        boolean existEmail = userRepository.findByEmail(requestDTO.email()).isPresent();
        if(existEmail) throw new CustomException(ExceptionCode.USER_EMAIL_EXIST);

        // 이메일 중복 체크 후 확인 버튼을 누르지 않고, 인위적으로 요청을 보내는 경우를 방지하고, 이를 검증하기 위해 토큰을 저장
        String token = UUID.randomUUID().toString();
        redisService.storeToken("emailCheckToken:" + token, " ", 10 * 60 * 1000L); // 10분 유효

        return new UserResponse.EmailTokenDTO(token);
    }

    @Transactional
    public void sendCode(UserRequest.SendCodeDTO requestDTO){
        // 중복 체크 후에 메일을 전송할 수 있도록, 토큰이 오면 이를 검증
        String token = "emailCheckToken:" + requestDTO.validationToken();

        if(!redisService.isTokenValid(token))
            throw new CustomException(ExceptionCode.BAD_APPROACH);
        redisService.removeToken(token); // 검증 후 토큰 삭제

        String verificationCode = generateVerificationCode();
        String subject = "[ForPaw] 이메일 인증 코드입니다.";
        String text = "인증 코드는 다음과 같습니다: " + verificationCode + "\n이 코드를 입력하여 이메일을 인증해 주세요.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(requestDTO.email());
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);

        // 레디스에 인증 코드 저장, 5분 동안 유효
        redisService.storeVerificationCode(requestDTO.email(), verificationCode, 5 * 60 * 1000L);
    }

    @Transactional
    public void verifyCode(UserRequest.VerifyCodeDTO requestDTO){


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