package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.GroupResponse;
import com.hong.ForPaw.controller.DTO.HomeResponse;
import com.hong.ForPaw.controller.DTO.PostResponse;
import com.hong.ForPaw.domain.Post.PostType;
import com.hong.ForPaw.domain.Province;
import com.hong.ForPaw.repository.Animal.AnimalRepository;
import com.hong.ForPaw.repository.Group.FavoriteGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final AnimalRepository animalRepository;
    private final GroupService groupService;
    private final RedisService redisService;
    private final PostService postService;
    private final AnimalService animalService;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private static final Province DEFAULT_PROVINCE = Province.DAEGU;

    @Transactional(readOnly = true)
    public HomeResponse.FindHomeDTO findHome(Long userId){
        // 1. 추천 동물
        List<Long> recommendedAnimalIds = animalService.getRecommendedAnimalIdList(userId);

        List<HomeResponse.AnimalDTO> animalDTOS = animalRepository.findAllByIds(recommendedAnimalIds).stream()
                .map(animal -> {
                    Long likeNum = redisService.getDataInLong("animalLikeNum", animal.getId().toString());

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
        List<PostResponse.PostDTO> postDTOS = postService.findPopularPostListByType(PostType.ADOPTION, 0);

        // 3. 추천 그룹
        List<Long> likedGroupIds = userId != null ? favoriteGroupRepository.findLikedGroupIdsByUserId(userId) : new ArrayList<>();
        List<GroupResponse.RecommendGroupDTO> groupDTOS = groupService.findRecommendGroupList(userId, DEFAULT_PROVINCE, likedGroupIds);

        return new HomeResponse.FindHomeDTO(animalDTOS, groupDTOS, postDTOS);
    }
}