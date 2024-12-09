package com.hong.forapw.service.like;

import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.service.RedisService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final RedisService redisService;
    private final PostLikeHandler postLikeHandler;
    private final CommentLikeHandler commentLikeHandler;

    private final Map<LikeTarget, LikeHandler> likeHandlers = Map.of(
            LikeTarget.POST, postLikeHandler,
            LikeTarget.COMMENT, commentLikeHandler
    );

    @Transactional
    public void likePost(Long postId, Long userId) {
        handleLike(postId, userId, LikeTarget.POST);
    }

    @Transactional
    public void likeComment(Long commentId, Long userId) {
        handleLike(commentId, userId, LikeTarget.COMMENT);
    }

    private void handleLike(Long targetId, Long userId, LikeTarget target) {
        LikeHandler handler = likeHandlers.get(target);
        Long ownerId = handler.findOwnerId(targetId);
        handler.validateNotSelfLike(ownerId, userId);

        String lockKey = handler.buildLockKey(targetId);
        executeWithLock(lockKey, () -> toggleLike(handler, targetId, userId));
    }

    private void executeWithLock(String lockKey, Runnable action) {
        RLock lock = redisService.getLock(lockKey);
        try {
            if (!tryToAcquireLock(lock)) {
                throw new CustomException(ExceptionCode.LOCK_ACQUIRE_FAIL);
            }
            action.run();
        } finally {
            safelyReleaseLock(lock);
        }
    }

    private boolean tryToAcquireLock(RLock lock) {
        try {
            return lock.tryLock(2, 5, TimeUnit.SECONDS); // 대기시간 2초, 락 유지시간 5초
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ExceptionCode.LOCK_ACQUIRE_INTERRUPT);
        }
    }

    private void safelyReleaseLock(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private void toggleLike(LikeHandler handler, Long targetId, Long userId) {
        if (handler.isAlreadyLiked(targetId, userId)) {
            handler.removeLike(targetId, userId);
        } else {
            handler.addLike(targetId, userId);
        }
    }
}

