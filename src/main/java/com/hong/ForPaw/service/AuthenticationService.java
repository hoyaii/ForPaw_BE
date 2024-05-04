package com.hong.ForPaw.service;

import com.hong.ForPaw.domain.Authentication.LoginAttempt;
import com.hong.ForPaw.repository.Authentication.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationService {

    private final LoginAttemptRepository loginAttemptRepository;

    @Transactional
    public void recordLoginAttempt(Long userId, String ip, String userAgent) {
        LoginAttempt attempt = LoginAttempt.builder()
                .userId(userId)
                .clientIp(ip)
                .userAgent(userAgent)
                .build();

        loginAttemptRepository.save(attempt);
    }
}

