package com.hong.forapw.service;

import com.hong.forapw.controller.dto.GroupResponse;
import com.hong.forapw.controller.dto.HomeResponse;
import com.hong.forapw.domain.post.PopularPost;
import com.hong.forapw.domain.Province;
import com.hong.forapw.domain.post.Post;
import com.hong.forapw.repository.animal.AnimalRepository;
import com.hong.forapw.repository.group.FavoriteGroupRepository;
import com.hong.forapw.repository.post.PopularPostRepository;
import com.hong.forapw.service.group.GroupService;
import com.hong.forapw.service.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.hong.forapw.core.utils.mapper.HomeMapper.toAnimalDTO;
import static com.hong.forapw.core.utils.mapper.HomeMapper.toPostDTO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final AnimalRepository animalRepository;
    private final PopularPostRepository popularPostRepository;
    private final GroupService groupService;
    private final LikeService likeService;
    private final AnimalService animalService;
    private final FavoriteGroupRepository favoriteGroupRepository;

    private static final Province DEFAULT_PROVINCE = Province.DAEGU;
    private static final String SORT_BY_DATE = "createdDate";
    private static final int POPULAR_POST_PAGE_INDEX = 0;
    private static final int POPULAR_POST_PAGE_SIZE = 5;

    public HomeResponse.FindHomeDTO findHomePageData(Long userId) {
        List<HomeResponse.AnimalDTO> recommendedAnimals = findRecommendedAnimals(userId);
        List<HomeResponse.PostDTO> popularPosts = findPopularPosts();
        List<GroupResponse.RecommendGroupDTO> recommendedGroups = findRecommendedGroups(userId);

        return new HomeResponse.FindHomeDTO(recommendedAnimals, recommendedGroups, popularPosts);
    }

    private List<HomeResponse.AnimalDTO> findRecommendedAnimals(Long userId) {
        List<Long> recommendedAnimalIds = animalService.findRecommendedAnimalIds(userId);

        return animalRepository.findByIds(recommendedAnimalIds).stream()
                .map(animal -> {
                    Long likeCount = likeService.getAnimalLikeCount(animal.getId());
                    return toAnimalDTO(animal, likeCount);
                })
                .toList();
    }

    private List<HomeResponse.PostDTO> findPopularPosts() {
        Pageable pageable = PageRequest.of(POPULAR_POST_PAGE_INDEX, POPULAR_POST_PAGE_SIZE, Sort.by(Sort.Direction.DESC, SORT_BY_DATE));
        List<PopularPost> popularPosts = popularPostRepository.findAllWithPost(pageable).getContent();

        return popularPosts.stream()
                .map(PopularPost::getPost)
                .map(post -> {
                    String imageURL = extractFirstImageUrl(post);
                    Long likeCount = likeService.getPostLikeCount(post.getId());
                    return toPostDTO(post, likeCount, imageURL);
                })
                .toList();
    }

    private String extractFirstImageUrl(Post post) {
        return post.getPostImages().isEmpty() ? null : post.getPostImages().get(0).getImageURL();
    }

    private List<GroupResponse.RecommendGroupDTO> findRecommendedGroups(Long userId) {
        List<Long> likedGroupIds = Optional.ofNullable(userId)
                .map(favoriteGroupRepository::findGroupIdByUserId)
                .orElse(Collections.emptyList());

        return groupService.findRecommendGroups(userId, DEFAULT_PROVINCE, likedGroupIds);
    }
}