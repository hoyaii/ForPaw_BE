package com.hong.forapw.service.like;

import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.domain.post.Comment;
import com.hong.forapw.domain.post.CommentLike;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.repository.post.CommentLikeRepository;
import com.hong.forapw.repository.post.CommentRepository;
import com.hong.forapw.service.RedisService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CommentLikeHandler implements LikeHandler {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final EntityManager entityManager;
    private final RedisService redisService;

    private static final String COMMENT_LIKE_NUM_KEY_PREFIX = "comment:like:count";
    private static final String COMMENT_LIKED_SET_KEY_PREFIX = "user:%s:liked_comments";
    private static final Long POST_CACHE_EXPIRATION_MS = 1000L * 60 * 60 * 24 * 90;

    @Override
    public void validateBeforeLike(Long commentId, Long userId) {
        Long ownerId = findOwnerId(commentId);
        validateNotSelfLike(ownerId, userId);
    }

    @Override
    public boolean isAlreadyLiked(Long commentId, Long userId) {
        return redisService.isMemberOfSet(buildUserLikedSetKey(userId), commentId.toString());
    }

    @Override
    public void addLike(Long commentId, Long userId) {
        User userRef = entityManager.getReference(User.class, userId);
        Comment commentRef = entityManager.getReference(Comment.class, commentId);

        CommentLike commentLike = CommentLike.builder().user(userRef).comment(commentRef).build();
        commentLikeRepository.save(commentLike);

        redisService.addSetElement(buildUserLikedSetKey(userId), commentId);
        redisService.incrementValue(COMMENT_LIKE_NUM_KEY_PREFIX, commentId.toString(), 1L);
    }

    @Override
    public void removeLike(Long commentId, Long userId) {
        Optional<CommentLike> commentLikeOP = commentLikeRepository.findByUserIdAndCommentId(userId, commentId);
        commentLikeOP.ifPresent(commentLikeRepository::delete);

        redisService.removeSetElement(buildUserLikedSetKey(userId), commentId.toString());
        redisService.decrementValue(COMMENT_LIKE_NUM_KEY_PREFIX, commentId.toString(), 1L);
    }

    @Override
    public Long getLikeCount(Long commentId) {
        Long likeCount = redisService.getValueInLongWithNull(COMMENT_LIKE_NUM_KEY_PREFIX, commentId.toString());
        if (likeCount == null) {
            likeCount = commentRepository.countLikesByCommentId(commentId);
            redisService.storeValue(COMMENT_LIKE_NUM_KEY_PREFIX, commentId.toString(), likeCount.toString(), POST_CACHE_EXPIRATION_MS);
        }

        return likeCount;
    }

    @Override
    public String buildLockKey(Long commentId) {
        return "comment:" + commentId + ":like:lock";
    }

    private String buildUserLikedSetKey(Long userId) {
        return String.format(COMMENT_LIKED_SET_KEY_PREFIX, userId);
    }

    private Long findOwnerId(Long commentId) {
        return commentRepository.findUserIdById(commentId)
                .orElseThrow(() -> new CustomException(ExceptionCode.COMMENT_NOT_FOUND));
    }

    private void validateNotSelfLike(Long ownerId, Long userId) {
        if (ownerId.equals(userId)) {
            throw new CustomException(ExceptionCode.CANT_LIKE_MY_POST);
        }
    }
}

