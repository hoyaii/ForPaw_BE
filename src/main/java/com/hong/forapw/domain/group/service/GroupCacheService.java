package com.hong.forapw.domain.group.service;

import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupCacheService {

    private final RedisService redisService;

    private static final String POST_READ_KEY_PREFIX = "user:readPosts:";

    public Set<String> getReadPostIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet(); // 유저 ID가 없는 경우 빈 Set 반환
        }

        String key = POST_READ_KEY_PREFIX + userId;
        return redisService.getMembersOfSet(key);
    }
}
