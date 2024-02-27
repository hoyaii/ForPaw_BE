package com.hong.ForPaw.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    // 토큰 저장, expirationTime은 토큰의 만료 시간(밀리초 단위)
    public void storeToken(String token, String value, Long expirationTime) {
        redisTemplate.opsForValue().set(token, value, expirationTime, TimeUnit.MILLISECONDS);
    }

    // 토큰 유효성 검사
    public boolean isTokenValid(String token) {
        return redisTemplate.hasKey(token);
    }

    // 토큰 삭제
    public void removeToken(String token) {
        redisTemplate.delete(token);
    }

    // 인증 코드 저장 (이메일)
    public void storeVerificationCode(String email, String verificationCode, Long expirationTime) {
        redisTemplate.opsForValue().set(buildVerificationCodeKey(email), verificationCode, expirationTime, TimeUnit.MINUTES);
    }

    // 인증 코드 유효성 검사
    public boolean isVerificationCodeValid(String email, String verificationCode) {
        String storedCode = redisTemplate.opsForValue().get(buildVerificationCodeKey(email));
        return verificationCode.equals(storedCode);
    }

    // 인증 코드 삭제
    public void removeVerificationCode(String email) {
        redisTemplate.delete(buildVerificationCodeKey(email));
    }

    // 이메일을 기반으로 한 Redis 키 생성
    private String buildVerificationCodeKey(String email) {
        return "verificationCode:" + email;
    }
}