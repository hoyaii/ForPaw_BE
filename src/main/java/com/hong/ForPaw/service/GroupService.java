package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.GroupRequest;
import com.hong.ForPaw.controller.DTO.GroupResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Group.FavoriteGroup;
import com.hong.ForPaw.domain.Group.Group;
import com.hong.ForPaw.domain.Group.GroupUser;
import com.hong.ForPaw.domain.Group.Role;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.repository.FavoriteGroupRepository;
import com.hong.ForPaw.repository.GroupRepository;
import com.hong.ForPaw.repository.GroupUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private final EntityManager entityManager;

    private Pageable pageableForMy = PageRequest.of(0, 1000);

    @Transactional
    public void createGroup(GroupRequest.CreateGroupDTO requestDTO, Long userId){
        // 이름 중복 체크
        if(groupRepository.findByName(requestDTO.name()).isPresent()){
            throw new CustomException(ExceptionCode.GROUP_NAME_EXIST);
        }

        Group group = Group.builder()
                .name(requestDTO.name())
                .region(requestDTO.region())
                .subRegion(requestDTO.subRegion())
                .description(requestDTO.description())
                .category(requestDTO.category())
                .profileURL(requestDTO.profileURL())
                .build();

        groupRepository.save(group);

        // 그룹장 설정
        User userRef = entityManager.getReference(User.class, userId);
        GroupUser groupUser = GroupUser.builder()
                .group(group)
                .user(userRef)
                .role(Role.ADMIN)
                .build();

        groupUserRepository.save(groupUser);
    }

    @Transactional
    public GroupResponse.FindGroupByIdDTO findGroupById(Long groupId, Long userId){
        // 조회 권한 체크 (수정을 위해 가져오는 정보니 권한 체크 필요)
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .ifPresentOrElse(groupUser -> {
                    if (!groupUser.getRole().equals(Role.ADMIN)) {
                        throw new CustomException(ExceptionCode.USER_FORBIDDEN);
                    }
                }, () -> {
                    throw new CustomException(ExceptionCode.USER_FORBIDDEN);
                });

        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        return new GroupResponse.FindGroupByIdDTO(group.getName(), group.getRegion(), group.getSubRegion(), group.getDescription(), group.getCategory(), group.getProfileURL());
    }

    @Transactional
    public void updateGroup(GroupRequest.UpdateGroupDTO requestDTO, Long groupId, Long userId){
        // 수정 권한 체크
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .ifPresentOrElse(groupUser -> {
                    if (!groupUser.getRole().equals(Role.ADMIN)) {
                        throw new CustomException(ExceptionCode.USER_FORBIDDEN);
                    }
                }, () -> {
                    throw new CustomException(ExceptionCode.USER_FORBIDDEN);
                });

        // 이름 중복 체크
        if(groupRepository.findByName(requestDTO.name()).isPresent()){
            throw new CustomException(ExceptionCode.GROUP_NAME_EXIST);
        }

        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        group.updateInfo(requestDTO.name(), requestDTO.region(), requestDTO.subRegion(), requestDTO.description(), requestDTO.category(), requestDTO.profileURL());
    }

    @Transactional
    public GroupResponse.FindAllGroupDTO findGroupList(Long userId, String region){
        // 이 API의 페이지네이션은 0페이지인 5개만 보내줄 것이다.
        Pageable pageable = createPageable(0, 5, "id");

        // 추천 그룹 찾기
        List<GroupResponse.RecommendGroupDTO> recommendGroupDTOS = getRecommendGroupDTOS(userId, region);

        // 지역 그룹 찾기
        List<GroupResponse.LocalGroupDTO> localGroupDTOS = getLocalGroupDTOS(userId, region, pageable);

        // 새 그룹 찾기
        List<GroupResponse.NewGroupDTO> newGroupDTOS = getNewGroupDTOS(userId, pageable);

        // 내 그룹 찾기
        List<GroupResponse.MyGroupDTO> myGroupDTOS = getMyGroupDTOS(userId, pageable);

        return new GroupResponse.FindAllGroupDTO(recommendGroupDTOS, newGroupDTOS, localGroupDTOS, myGroupDTOS);
    }

    @Transactional
    public GroupResponse.FindLocalGroupDTO findLocalGroup(Long userId, String region, Integer page, Integer size){

        Pageable pageable = createPageable(page, size, "id");
        List<GroupResponse.LocalGroupDTO> localGroupDTOS = getLocalGroupDTOS(userId, region, pageable);

        return new GroupResponse.FindLocalGroupDTO(localGroupDTOS);
    }

    @Transactional
    public GroupResponse.FindNewGroupDTO findNewGroup(Long userId, Integer page, Integer size){

        Pageable pageable = createPageable(page, size, "id");
        List<GroupResponse.NewGroupDTO> newGroupDTOS = getNewGroupDTOS(userId, pageable);

        return new GroupResponse.FindNewGroupDTO(newGroupDTOS);
    }

    @Transactional
    public GroupResponse.FindMyGroupDTO findMyGroup(Long userId, Integer page, Integer size){

        Pageable pageable = createPageable(page, size, "id");
        List<GroupResponse.MyGroupDTO> myGroupDTOS = getMyGroupDTOS(userId, pageable);

        return new GroupResponse.FindMyGroupDTO(myGroupDTOS);
    }

    @Transactional
    public void joinGroup(GroupRequest.JoinGroupDTO requestDTO, Long userId, Long groupId){
        // 존재하지 않는 그룹이면 에러
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );
        User userRef = entityManager.getReference(User.class, userId);

        Optional<GroupUser> groupUserOP = groupUserRepository.findByGroupIdAndUserId(groupId, userId);

        if(groupUserOP.isPresent()){
            throw new CustomException(ExceptionCode.GROUP_ALREADY_JOIN);
        }
        else{
            GroupUser groupUser = GroupUser.builder()
                    .role(Role.TEMP)
                    .user(userRef)
                    .group(group)
                    .greeting(requestDTO.greeting())
                    .build();
            groupUserRepository.save(groupUser);
        }
    }

    @Transactional
    public void likeGroup(Long userId, Long groupId){
        // 존재하지 않는 그룹이면 에러
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );
        User userRef = entityManager.getReference(User.class, userId);

        Optional<FavoriteGroup> favoriteGroupOP = favoriteGroupRepository.findByUserIdAndGroupId(userId, groupId);

        // 좋아요가 이미 있다면 삭제, 없다면 추가
        if (favoriteGroupOP.isPresent()) {
            favoriteGroupRepository.delete(favoriteGroupOP.get());
        }
        else {
            FavoriteGroup favoriteGroup = FavoriteGroup.builder()
                    .user(userRef)
                    .group(group)
                    .build();
            favoriteGroupRepository.save(favoriteGroup);
        }
    }

    public List<GroupResponse.RecommendGroupDTO> getRecommendGroupDTOS(Long userId, String region){
        // 내가 가입한 그룹
        Set<Long> myGroupIds = getMyGroups(userId, pageableForMy).stream()
                .map(Group::getId)
                .collect(Collectors.toSet());

        // 1. 같은 지역의 그룹  2. 좋아요, 사용자 순  3. 비슷한 연관관계 (카테고리, 설명) => 3번은 AI를 사용해야 하기 때문에 일단은 1과 2의 기준으로 추천
        Sort sort = Sort.by(Sort.Order.desc("likeNum"), Sort.Order.desc("participationNum"));
        Pageable pageableForRecommend = PageRequest.of(0, 1000, sort);

        Page<Group> recommendGroups = groupRepository.findByRegion(region, pageableForRecommend);
        List<GroupResponse.RecommendGroupDTO> allRecommendGroupDTOS = recommendGroups.getContent().stream()
                .filter(group -> !myGroupIds.contains(group.getId())) // 내가 가입한 그룹을 제외
                .map(group -> new GroupResponse.RecommendGroupDTO(group.getId(), group.getName(), group.getDescription(), group.getParticipationNum(), group.getCategory() ,group.getRegion(), group.getSubRegion(), group.getProfileURL(), group.getLikeNum()))
                .collect(Collectors.toList());

        // 매번 동일하게 추천을 할 수는 없으니, 간추린 추천 목록 중에서 5개를 랜덤으로 보내준다.
        Collections.shuffle(allRecommendGroupDTOS);
        List<GroupResponse.RecommendGroupDTO> recommendGroupDTOS = allRecommendGroupDTOS.stream()
                .limit(5)
                .collect(Collectors.toList());

        return recommendGroupDTOS;
    }

    public List<GroupResponse.LocalGroupDTO> getLocalGroupDTOS(Long userId, String region, Pageable pageable){
        // 내가 가입한 그룹
        Set<Long> myGroupIds = getMyGroups(userId, pageableForMy).stream()
                .map(Group::getId)
                .collect(Collectors.toSet());

        Page<Group> localGroups = groupRepository.findByRegion(region, pageable);
        List<GroupResponse.LocalGroupDTO> localGroupDTOS = localGroups.getContent().stream()
                .filter(group -> !myGroupIds.contains(group.getId())) // 내가 가입한 그룹을 제외
                .map(group -> new GroupResponse.LocalGroupDTO(group.getId(), group.getName(), group.getDescription(), group.getParticipationNum(), group.getCategory(), group.getRegion(), group.getSubRegion(), group.getProfileURL(), group.getLikeNum()))
                .collect(Collectors.toList());

        return localGroupDTOS;
    }

    public List<GroupResponse.NewGroupDTO> getNewGroupDTOS(Long userId, Pageable pageable){
        // 내가 가입한 그룹
        Set<Long> myGroupIds = getMyGroups(userId, pageableForMy).stream()
                .map(Group::getId)
                .collect(Collectors.toSet());

        Page<Group> newGroups = groupRepository.findAll(pageable);
        List<GroupResponse.NewGroupDTO> newGroupDTOS = newGroups.getContent().stream()
                .filter(group -> !myGroupIds.contains(group.getId())) // 내가 가입한 그룹을 제외
                .map(group -> new GroupResponse.NewGroupDTO(group.getId(), group.getName(), group.getCategory(), group.getRegion(), group.getSubRegion(), group.getProfileURL()))
                .collect(Collectors.toList());

        return newGroupDTOS;
    }

    public List<GroupResponse.MyGroupDTO> getMyGroupDTOS(Long userId, Pageable pageable){

        List<Group> myGroups = getMyGroups(userId, pageable);

        List<GroupResponse.MyGroupDTO> myGroupDTOS = myGroups.stream()
                .map(group -> new GroupResponse.MyGroupDTO(group.getId(), group.getName(), group.getDescription(),
                        group.getParticipationNum(), group.getCategory(), group.getRegion(), group.getSubRegion(), group.getProfileURL(), group.getLikeNum()))
                .collect(Collectors.toList());

        return myGroupDTOS;
    }

    public List<Group> getMyGroups(Long userId, Pageable pageable){

        Page<GroupUser> groupUsers = groupUserRepository.findByUserId(userId, pageable);
        List<Group> myGroups = groupUsers.getContent().stream()
                .map(GroupUser::getGroup)
                .collect(Collectors.toList());

        return myGroups;
    }

    private Pageable createPageable(int page, int size, String sortProperty) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortProperty));
    }
}