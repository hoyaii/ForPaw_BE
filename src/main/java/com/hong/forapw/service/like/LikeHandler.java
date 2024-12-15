package com.hong.forapw.service.like;

public interface LikeHandler {
    void initCount(Long targetId);

    void validateBeforeLike(Long targetId, Long userId);

    boolean isAlreadyLiked(Long targetId, Long userId);

    void addLike(Long targetId, Long userId);

    void removeLike(Long targetId, Long userId);

    Long getLikeCount(Long targetId);

    String buildLockKey(Long targetId);

    void clear(Long targetId);
}

