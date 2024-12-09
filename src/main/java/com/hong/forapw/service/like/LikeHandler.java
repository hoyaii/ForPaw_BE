package com.hong.forapw.service.like;

public interface LikeHandler {
    Long findOwnerId(Long targetId);
    void validateNotSelfLike(Long ownerId, Long userId);
    boolean isAlreadyLiked(Long targetId, Long userId);
    void addLike(Long targetId, Long userId);
    void removeLike(Long targetId, Long userId);
    String buildLockKey(Long targetId);
}

