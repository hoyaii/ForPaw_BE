package com.hong.forapw.service.like;

import com.hong.forapw.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final RedisService redisService;
    private final PostLikeHandler postLikeHandler;
    private final CommentLikeHandler commentLikeHandler;
    private final GroupLikeHandler groupLikeHandler;

    private final Map<LikeTarget, LikeHandler> likeHandlers = Map.of(
            LikeTarget.POST, postLikeHandler,
            LikeTarget.COMMENT, commentLikeHandler
    );
    private final AnimalLikeHandler animalLikeHandler;

    @Transactional
    public void likePost(Long postId, Long userId) {
        handleLike(postId, userId, LikeTarget.POST);
    }

    @Transactional
    public void likeComment(Long commentId, Long userId) {
        handleLike(commentId, userId, LikeTarget.COMMENT);
    }

    @Transactional
    public void likeAnimal(Long animalId, Long userId) {
        handleLike(animalId, userId, LikeTarget.ANIMAL);
    }

    @Transactional
    public void likeGroup(Long groupId, Long userId) {
        handleLike(groupId, userId, LikeTarget.GROUP);
    }

    @Transactional(readOnly = true)
    public Long getPostLikeCount(Long postId) {
        return postLikeHandler.getLikeCount(postId);
    }

    @Transactional(readOnly = true)
    public Long getCommentLikeCount(Long commentId) {
        return commentLikeHandler.getLikeCount(commentId);
    }

    @Transactional(readOnly = true)
    public Long getAnimalLikeCount(Long animalId) {
        return animalLikeHandler.getLikeCount(animalId);
    }

    @Transactional(readOnly = true)
    public Long getGroupLikeCount(Long groupId) {
        return groupLikeHandler.getLikeCount(groupId);
    }

    private void handleLike(Long targetId, Long userId, LikeTarget target) {
        LikeHandler handler = likeHandlers.get(target);
        handler.validateBeforeLike(targetId, userId);

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

