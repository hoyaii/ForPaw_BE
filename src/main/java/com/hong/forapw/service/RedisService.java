package com.hong.forapw.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public void storeValue(String type, String id, String value, Long expirationTime) {
        redisTemplate.opsForValue().set(buildKey(type, id), value, expirationTime, TimeUnit.MILLISECONDS);
    }

    public void storeValue(String type, String id, String value) {
        redisTemplate.opsForValue().set(buildKey(type, id), value);
    }

    public void addSetElement(String key, Long value) {
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        setOps.add(key, String.valueOf(value));
    }

    public void addSetElement(String key, Long value, Long expirationTime) {
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        setOps.add(key, String.valueOf(value));
        redisTemplate.expire(key, expirationTime, TimeUnit.SECONDS);
    }

    public void addListElement(String key, String value, Long limit) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        listOps.leftPush(key, value);
        listOps.trim(key, 0, limit - 1);
    }

    public void incrementValue(String type, String id, Long value) {
        redisTemplate.opsForValue().increment(buildKey(type, id), value);
    }

    public void decrementValue(String type, String id, Long value) {
        redisTemplate.opsForValue().decrement(buildKey(type, id), value);
    }

    public void setExpireDate(String type, String id, Long expirationTime) {
        redisTemplate.expire(buildKey(type, id), expirationTime, TimeUnit.MILLISECONDS);
    }

    public void removeValue(String type, String id) {
        redisTemplate.delete(buildKey(type, id));
    }

    public void removeValue(String key) {
        redisTemplate.delete(key);
    }

    public void removeListElement(String key, String value) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        listOps.remove(key, 0, value);
    }

    public boolean isValueExist(String type, String id) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(type, id)));
    }

    public boolean isValueNotExist(String type, String id) {
        return !Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(type, id)));
    }

    public boolean isNotStoredValue(String type, String id, String value) {
        String storedValue = redisTemplate.opsForValue().get(buildKey(type, id));
        return (storedValue == null) || !storedValue.equals(value);
    }

    public boolean isStoredValue(String type, String id, String value) {
        String storedValue = redisTemplate.opsForValue().get(buildKey(type, id));
        return storedValue != null && storedValue.equals(value);
    }

    public String getValueInString(String type, String id) {
        return redisTemplate.opsForValue().get(buildKey(type, id));
    }

    public Long getValueInLong(String type, String id) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(buildKey(type, id)))
                .map(Long::valueOf)
                .orElse(0L);
    }

    public Long getValueInLongWithNull(String type, String id) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(buildKey(type, id)))
                .map(Long::valueOf)
                .orElse(null);
    }

    public Set<String> getMembersOfSet(String key) {
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        return setOps.members(key);
    }

    private String buildKey(String type, String id) {
        return type + ":" + id;
    }
}