package com.hong.forapw.service;

import com.hong.forapw.controller.dto.GroupResponse;
import com.hong.forapw.controller.dto.HomeResponse;
import com.hong.forapw.domain.post.PopularPost;
import com.hong.forapw.domain.Province;
import com.hong.forapw.repository.animal.AnimalRepository;
import com.hong.forapw.repository.group.FavoriteGroupRepository;
import com.hong.forapw.repository.post.PopularPostRepository;
import com.hong.forapw.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final AnimalRepository animalRepository;
    private final PostRepository postRepository;
    private final PopularPostRepository popularPostRepository;
    private final GroupService groupService;
    private final RedisService redisService;
    private final AnimalService animalService;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private static final Province DEFAULT_PROVINCE = Province.DAEGU;
    public static final Long POST_EXP = 1000L * 60 * 60 * 24 * 90; // 세 달
    public static final Long ANIMAL_EXP = 1000L * 60 * 60 * 24 * 90; // 세 달
    private static final String POST_LIKE_NUM_KEY_PREFIX = "postLikeNum";
    private static final String ANIMAL_LIKE_NUM_KEY_PREFIX = "animalLikeNum";
    private static final String SORT_BY_DATE = "createdDate";

    @Transactional(readOnly = true)
    public HomeResponse.FindHomeDTO findHome(Long userId) {
        // 1. 추천 동물
        List<Long> recommendedAnimalIds = animalService.getRecommendedAnimalIdList(userId);

        List<HomeResponse.AnimalDTO> animalDTOS = animalRepository.findByIds(recommendedAnimalIds).stream()
                .map(animal -> {
                    Long likeNum = getCachedAnimalLikeNum(ANIMAL_LIKE_NUM_KEY_PREFIX, animal.getId());
                    return new HomeResponse.AnimalDTO(
                            animal.getId(),
                            animal.getName(),
                            animal.getAge(),
                            animal.getGender(),
                            animal.getSpecialMark(),
                            animal.getRegion(),
                            animal.getInquiryNum(),
                            likeNum,
                            animal.getProfileURL());
                })
                .toList();

        // 2. 인기 글
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, SORT_BY_DATE));
        Page<PopularPost> popularPostPage = popularPostRepository.findAllWithPost(pageable);
        List<HomeResponse.PostDTO> postDTOS = popularPostPage.getContent().stream()
                .map(PopularPost::getPost)
                .map(post -> {
                    String imageURL = post.getPostImages().isEmpty() ? null : post.getPostImages().get(0).getImageURL();
                    Long likeNum = getCachedPostLikeNum(POST_LIKE_NUM_KEY_PREFIX, post.getId());

                    return new HomeResponse.PostDTO(
                            post.getId(),
                            post.getUser().getNickname(),
                            post.getTitle(),
                            post.getContent(),
                            post.getCreatedDate(),
                            post.getCommentNum(),
                            likeNum,
                            imageURL,
                            post.getPostType());
                })
                .toList();

        // 3. 추천 그룹
        List<Long> likedGroupIds = userId != null ? favoriteGroupRepository.findGroupIdByUserId(userId) : new ArrayList<>();
        List<GroupResponse.RecommendGroupDTO> groupDTOS = groupService.findRecommendGroupList(userId, DEFAULT_PROVINCE, likedGroupIds);

        return new HomeResponse.FindHomeDTO(animalDTOS, groupDTOS, postDTOS);
    }

    private Long getCachedPostLikeNum(String keyPrefix, Long key) {
        Long likeNum = redisService.getValueInLongWithNull(keyPrefix, key.toString());

        if (likeNum == null) {
            likeNum = postRepository.countLikesByPostId(key);
            redisService.storeValue(keyPrefix, key.toString(), likeNum.toString(), POST_EXP);
        }

        return likeNum;
    }

    private Long getCachedAnimalLikeNum(String keyPrefix, Long key) {
        Long likeNum = redisService.getValueInLongWithNull(keyPrefix, key.toString());

        if (likeNum == null) {
            likeNum = animalRepository.countLikesByAnimalId(key);
            redisService.storeValue(keyPrefix, key.toString(), likeNum.toString(), ANIMAL_EXP);
        }

        return likeNum;
    }
}