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
            redisService.tryToAcquireLock(lock, 2, 5, TimeUnit.SECONDS);
            action.run();
        } finally {
            redisService.safelyReleaseLock(lock);
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

