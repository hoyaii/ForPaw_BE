package com.hong.forapw.domain.post.service;

import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final RedisService redisService;

    /** 게시글 좋아요/뷰 카운트를 캐싱하는 기간 (3개월) */
    private static final Long POST_CACHE_EXPIRATION = 1000L * 60 * 60 * 24 * 90;
    /** 공지사항 읽음 상태 캐싱 기간 (1년) */
    private static final Long NOTICE_READ_EXPIRATION = 60L * 60 * 24 * 360;
    private static final String REDIS_POST_LIKE_COUNT_KEY_PREFIX = "post:like:count:";
    private static final String REDIS_COMMENT_LIKE_COUNT_KEY_PREFIX = "comment:like:count:";
    private static final String REDIS_POST_READ_KEY_PREFIX = "user:readPosts:";
    private static final String REDIS_POST_VIEW_COUNT_KEY_PREFIX = "postViewNum:";

    public void initializePostCache(Long postId) {
        redisService.storeValue(REDIS_POST_LIKE_COUNT_KEY_PREFIX, postId.toString(), "0", POST_CACHE_EXPIRATION);
        redisService.storeValue(REDIS_POST_VIEW_COUNT_KEY_PREFIX, postId.toString(), "0", POST_CACHE_EXPIRATION);
    }

    public void initializeCommentCache(Long commentId) {
        redisService.storeValue(REDIS_COMMENT_LIKE_COUNT_KEY_PREFIX, commentId.toString(), "0", POST_CACHE_EXPIRATION);
    }

    public void markNoticePostAsRead(Post post, Long userId, Long postId) {
        if (post.isNoticeType()) {
            String key = REDIS_POST_READ_KEY_PREFIX + userId;
            redisService.addSetElement(key, postId, NOTICE_READ_EXPIRATION);
        }
    }

    public void incrementPostViewCount(Long postId) {
        redisService.incrementValue(REDIS_POST_VIEW_COUNT_KEY_PREFIX, postId.toString(), 1L);
    }

    public Long getPostViewCount(Long postId, Post post) {
        Long viewCount = redisService.getValueInLongWithNull(REDIS_POST_VIEW_COUNT_KEY_PREFIX, postId.toString());
        if (viewCount == null) {
            viewCount = post.getReadCnt();
            redisService.storeValue(REDIS_POST_VIEW_COUNT_KEY_PREFIX, postId.toString(), viewCount.toString(), POST_CACHE_EXPIRATION);
        }
        return viewCount;
    }

    public Long getPostViewCount(Post post) {
        return redisService.getValueInLongWithNull(REDIS_POST_VIEW_COUNT_KEY_PREFIX, post.getId().toString());
    }
}
