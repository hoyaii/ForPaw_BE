package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.GroupResponse;
import com.hong.ForPaw.controller.DTO.HomeResponse;
import com.hong.ForPaw.controller.DTO.PostResponse;
import com.hong.ForPaw.domain.Post.PopularPost;
import com.hong.ForPaw.domain.Post.PostType;
import com.hong.ForPaw.domain.Province;
import com.hong.ForPaw.repository.Animal.AnimalRepository;
import com.hong.ForPaw.repository.Group.FavoriteGroupRepository;
import com.hong.ForPaw.repository.Post.PopularPostRepository;
import com.hong.ForPaw.repository.Post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final AnimalRepository animalRepository;
    private final PopularPostRepository popularPostRepository;
    private final GroupService groupService;
    private final RedisService redisService;
    private final AnimalService animalService;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private static final Province DEFAULT_PROVINCE = Province.DAEGU;
    private static final String POST_LIKE_NUM_KEY_PREFIX = "postLikeNum";
    private static final String ANIMAL_LIKE_NUM_KEY_PREFIX = "animalLikeNum";
    private static final String SORT_BY_DATE = "createdDate";

    @Transactional(readOnly = true)
    public HomeResponse.FindHomeDTO findHome(Long userId){
        // 1. 추천 동물
        List<Long> recommendedAnimalIds = animalService.getRecommendedAnimalIdList(userId);

        List<HomeResponse.AnimalDTO> animalDTOS = animalRepository.findByIds(recommendedAnimalIds).stream()
                .map(animal -> {
                    Long likeNum = redisService.getValueInLong(ANIMAL_LIKE_NUM_KEY_PREFIX, animal.getId().toString());

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
                    Long likeNum = getCachedLikeNum(POST_LIKE_NUM_KEY_PREFIX, post.getId(), post::getLikeNum);

                    return new HomeResponse.PostDTO(
                            post.getId(),
                            post.getUser().getNickName(),
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

    private Long getCachedLikeNum(String keyPrefix, Long key, Supplier<Long> dbFallback) {
        Long likeNum = redisService.getValueInLong(keyPrefix, key.toString());

        if (likeNum == null) {
            likeNum = dbFallback.get();
        }
        return likeNum;
    }
}