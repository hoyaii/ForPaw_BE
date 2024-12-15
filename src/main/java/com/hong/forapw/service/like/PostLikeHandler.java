package com.hong.forapw.service.like;

import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.domain.post.Post;
import com.hong.forapw.domain.post.PostLike;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.repository.post.PostLikeRepository;
import com.hong.forapw.repository.post.PostRepository;
import com.hong.forapw.service.RedisService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostLikeHandler implements LikeHandler {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final EntityManager entityManager;
    private final RedisService redisService;

    private static final String POST_LIKE_NUM_KEY_PREFIX = "post:like:count";
    private static final String POST_LIKED_SET_KEY_PREFIX = "user:%s:liked_posts";
    private static final Long POST_CACHE_EXPIRATION_MS = 1000L * 60 * 60 * 24 * 90;

    @Override
    public void initCount(Long targetId) {

    }

    @Override
    public void validateBeforeLike(Long postId, Long userId) {
        Long ownerId = findOwnerId(postId);
        validateNotSelfLike(ownerId, userId);
    }

    @Override
    public boolean isAlreadyLiked(Long postId, Long userId) {
        return redisService.isMemberOfSet(buildUserLikedSetKey(userId), postId.toString());
    }

    @Override
    public void addLike(Long postId, Long userId) {
        User userRef = entityManager.getReference(User.class, userId);
        Post postRef = entityManager.getReference(Post.class, postId);

        PostLike postLike = PostLike.builder().user(userRef).post(postRef).build();
        postLikeRepository.save(postLike);

        redisService.addSetElement(buildUserLikedSetKey(userId), postId);
        redisService.incrementValue(POST_LIKE_NUM_KEY_PREFIX, postId.toString(), 1L);
    }

    @Override
    public void removeLike(Long postId, Long userId) {
        Optional<PostLike> postLikeOP = postLikeRepository.findByUserIdAndPostId(userId, postId);
        postLikeOP.ifPresent(postLikeRepository::delete);

        redisService.removeSetElement(buildUserLikedSetKey(userId), postId.toString());
        redisService.decrementValue(POST_LIKE_NUM_KEY_PREFIX, postId.toString(), 1L);
    }

    @Override
    public Long getLikeCount(Long postId) {
        Long likeCount = redisService.getValueInLongWithNull(POST_LIKE_NUM_KEY_PREFIX, postId.toString());
        if (likeCount == null) {
            likeCount = postRepository.countLikesByPostId(postId);
            redisService.storeValue(POST_LIKE_NUM_KEY_PREFIX, postId.toString(), likeCount.toString(), POST_CACHE_EXPIRATION_MS);
        }
        return likeCount;
    }

    @Override
    public String buildLockKey(Long postId) {
        return "post:" + postId + ":like:lock";
    }

    @Override
    public void clear(Long targetId) {

    }

    private String buildUserLikedSetKey(Long userId) {
        return String.format(POST_LIKED_SET_KEY_PREFIX, userId);
    }

    private Long findOwnerId(Long postId) {
        return postRepository.findUserIdById(postId)
                .orElseThrow(() -> new CustomException(ExceptionCode.POST_NOT_FOUND));
    }

    private void validateNotSelfLike(Long ownerId, Long userId) {
        if (ownerId.equals(userId)) {
            throw new CustomException(ExceptionCode.CANT_LIKE_MY_POST);
        }
    }
}

