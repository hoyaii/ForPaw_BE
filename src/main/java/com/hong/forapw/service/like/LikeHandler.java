package com.hong.forapw.service.like;

public interface LikeHandler {
    void validateBeforeLike(Long targetId, Long userId);

    boolean isAlreadyLiked(Long targetId, Long userId);

    void addLike(Long targetId, Long userId);

    void removeLike(Long targetId, Long userId);

    String buildLockKey(Long targetId);
}

