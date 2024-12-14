package com.hong.forapw.service.like;

import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.domain.group.FavoriteGroup;
import com.hong.forapw.domain.group.Group;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.repository.UserRepository;
import com.hong.forapw.repository.group.FavoriteGroupRepository;
import com.hong.forapw.repository.group.GroupRepository;
import com.hong.forapw.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GroupLikeHandler implements LikeHandler {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private final RedisService redisService;

    private static final String GROUP_LIKE_NUM_KEY_PREFIX = "group:like:count";
    private static final String GROUP_LIKED_SET_KEY_PREFIX = "user:%s:liked_groups";
    public static final Long GROUP_CACHE_EXPIRATION_MS = 1000L * 60 * 60 * 24 * 90; // 세 달

    @Override
    public void validateBeforeLike(Long groupId, Long userId) {
        if (!groupRepository.existsById(groupId)) {
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }
    }

    @Override
    public boolean isAlreadyLiked(Long groupId, Long userId) {
        return redisService.isMemberOfSet(buildUserLikedSetKey(userId), groupId.toString());
    }

    @Override
    public void addLike(Long groupId, Long userId) {
        Group group = groupRepository.getReferenceById(groupId);
        User user = userRepository.getReferenceById(userId);

        FavoriteGroup favoriteGroup = FavoriteGroup.builder()
                .user(user)
                .group(group)
                .build();
        favoriteGroupRepository.save(favoriteGroup);

        redisService.addSetElement(buildUserLikedSetKey(userId), groupId);
        redisService.incrementValue(GROUP_LIKE_NUM_KEY_PREFIX, groupId.toString(), 1L);
    }

    @Override
    public void removeLike(Long groupId, Long userId) {
        Optional<FavoriteGroup> favoriteGroupOP = favoriteGroupRepository.findByUserIdAndGroupId(userId, groupId);
        favoriteGroupOP.ifPresent(favoriteGroupRepository::delete);

        redisService.removeSetElement(buildUserLikedSetKey(userId), groupId.toString());
        redisService.decrementValue(GROUP_LIKE_NUM_KEY_PREFIX, groupId.toString(), 1L);
    }

    @Override
    public Long getLikeCount(Long groupId) {
        Long likeCount = redisService.getValueInLongWithNull(GROUP_LIKE_NUM_KEY_PREFIX, groupId.toString());
        if (likeCount == null) {
            likeCount = groupRepository.countLikesByGroupId(groupId);
            redisService.storeValue(GROUP_LIKE_NUM_KEY_PREFIX, groupId.toString(), likeCount.toString(), GROUP_CACHE_EXPIRATION_MS);
        }

        return likeCount;
    }

    @Override
    public String buildLockKey(Long groupId) {
        return "group:" + groupId + ":like:lock";
    }

    private String buildUserLikedSetKey(Long userId) {
        return String.format(GROUP_LIKED_SET_KEY_PREFIX, userId);
    }
}
