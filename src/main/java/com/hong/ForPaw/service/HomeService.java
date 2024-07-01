package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.HomeResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Animal.Animal;
import com.hong.ForPaw.domain.Group.Group;
import com.hong.ForPaw.domain.Post.Post;
import com.hong.ForPaw.domain.Province;
import com.hong.ForPaw.repository.Animal.AnimalRepository;
import com.hong.ForPaw.repository.Group.FavoriteGroupRepository;
import com.hong.ForPaw.repository.Group.GroupRepository;
import com.hong.ForPaw.repository.Group.GroupUserRepository;
import com.hong.ForPaw.repository.Post.PostRepository;
import com.hong.ForPaw.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final PostRepository postRepository;
    private final GroupRepository groupRepository;
    private final AnimalRepository animalRepository;
    private final GroupUserRepository groupUserRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private static final Province DEFAULT_PROVINCE = Province.DAEGU;

    @Transactional
    public HomeResponse.FindHomeDTO findHome(Long userId){
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"));

        Page<Animal> animalPage = animalRepository.findAll(pageable);
        List<HomeResponse.AnimalDTO> animalDTOS = animalPage.getContent().stream()
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
                            animal.getProfileURL()
                    );
                })
                .toList();

        Page<Post> postPage = postRepository.findWithUser(pageable);
        List<HomeResponse.PostDTO> postDTOS = postPage.getContent().stream()
                .map(post -> {
                    // 캐싱 기간이 지나 캐싱이 불가능하면, DB에서 조회
                    Long likeNum = redisService.getDataInLongWithNull("postLikeNum", post.getId().toString());
                    if(likeNum == null){
                        likeNum = post.getLikeNum();
                    }

                    return new HomeResponse.PostDTO(
                            post.getId(),
                            post.getUser().getNickName(),
                            post.getTitle(),
                            post.getContent(),
                            post.getCreatedDate(),
                            post.getCommentNum(),
                            likeNum,
                            post.getPostImages().get(0).getImageURL()
                    );
                })
                .toList();

        // 로그인이 되어 있으면, 가입 시 기재한 주소를 바탕으로 그룹 조회
        Province province = DEFAULT_PROVINCE;

        if (userId != null) {
            province = userRepository.findProvinceById(userId)
                    .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        }

        // 만약 로그인 되어 있지 않다면, 빈 셋으로 처리한다.
        List<Long> likedGroupIds = userId != null ? favoriteGroupRepository.findLikedGroupIdsByUserId(userId) : new ArrayList<>();
        Set<Long> joinedGroupIds = userId != null ? getGroupIdSet(userId) : Collections.emptySet();

        Page<Group> recommendGroups = groupRepository.findByProvince(province, getRecommendPageable());
        List<HomeResponse.GroupDTO> groupDTOS = recommendGroups.getContent().stream()
                .filter(group -> !joinedGroupIds.contains(group.getId()))
                .map(group -> {
                    Long likeNum = redisService.getDataInLong("groupLikeNum", group.getId().toString());

                    return new HomeResponse.GroupDTO(
                            group.getId(),
                            group.getName(),
                            group.getDescription(),
                            group.getParticipantNum(),
                            group.getCategory(),
                            group.getProvince(),
                            group.getDistrict(),
                            group.getProfileURL(),
                            likeNum,
                            likedGroupIds.contains(group.getId())
                    );
                })
                .toList();

        return new HomeResponse.FindHomeDTO(animalDTOS, groupDTOS, postDTOS);
    }

    private Set<Long> getGroupIdSet(Long userId){
        List<Group> groups = groupUserRepository.findAllGroupByUserId(userId);

        Set<Long> groupIdSet = groups.stream()
                .map(Group::getId)
                .collect(Collectors.toSet());

        return groupIdSet;
    }

    private Pageable getRecommendPageable(){
        // 1. 같은 지역의 그룹  2. 좋아요, 사용자 순
        Sort sort = Sort.by(Sort.Order.desc("likeNum"), Sort.Order.desc("participantNum"));
        Pageable pageable = PageRequest.of(0, 30, sort);

        return pageable;
    }
}