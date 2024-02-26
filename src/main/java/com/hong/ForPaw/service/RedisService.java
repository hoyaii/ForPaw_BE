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
    // set(): 주어진 key(token)에 value("")를 저장하며, 만료 시간을 설정. 여기서 value는 비어 있는 문자열로 설정
    public void storeToken(String token, Long expirationTime) {
        redisTemplate.opsForValue().set(token, "", expirationTime, TimeUnit.MILLISECONDS);
    }

    // 토큰 유효성 검사
    public boolean isTokenValid(String token) {
        return redisTemplate.hasKey(token);
    }

    // 토큰 삭제
    public void removeToken(String token) {
        redisTemplate.delete(token);
    }
}