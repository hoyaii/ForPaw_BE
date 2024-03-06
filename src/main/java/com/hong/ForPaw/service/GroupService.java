package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.GroupRequest;
import com.hong.ForPaw.controller.DTO.GroupResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Group.*;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingUserRepository meetingUserRepository;
    private final EntityManager entityManager;

    private Pageable pageableForMy = PageRequest.of(0, 1000);

    @Transactional
    public void createGroup(GroupRequest.CreateGroupDTO requestDTO, Long userId){
        // 이름 중복 체크
        if(!groupRepository.existsByName(requestDTO.name())){
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
        checkAuthority(groupId, userId);

        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ExceptionCode.GROUP_NOT_FOUND)
        );

        return new GroupResponse.FindGroupByIdDTO(group.getName(), group.getRegion(), group.getSubRegion(), group.getDescription(), group.getCategory(), group.getProfileURL());
    }

    @Transactional
    public void updateGroup(GroupRequest.UpdateGroupDTO requestDTO, Long groupId, Long userId){
        // 수정 권한 체크
        checkAuthority(groupId, userId);

        // 이름 중복 체크
        if(groupRepository.existsByName(requestDTO.name())){
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

    // 지역 그룹 추가 조회
    @Transactional
    public GroupResponse.FindLocalGroupDTO findLocalGroup(Long userId, String region, Integer page, Integer size){

        Pageable pageable = createPageable(page, size, "id");
        List<GroupResponse.LocalGroupDTO> localGroupDTOS = getLocalGroupDTOS(userId, region, pageable);

        if(localGroupDTOS.isEmpty()){
            throw new CustomException(ExceptionCode.SEARCH_NOT_FOUND);
        }

        return new GroupResponse.FindLocalGroupDTO(localGroupDTOS);
    }

    // 새 그룹 추가 조회
    @Transactional
    public GroupResponse.FindNewGroupDTO findNewGroup(Long userId, Integer page, Integer size){

        Pageable pageable = createPageable(page, size, "id");
        List<GroupResponse.NewGroupDTO> newGroupDTOS = getNewGroupDTOS(userId, pageable);

        if(newGroupDTOS.isEmpty()){
            throw new CustomException(ExceptionCode.SEARCH_NOT_FOUND);
        }

        return new GroupResponse.FindNewGroupDTO(newGroupDTOS);
    }

    // 내 그룹 추가 조회
    @Transactional
    public GroupResponse.FindMyGroupDTO findMyGroup(Long userId, Integer page, Integer size){

        Pageable pageable = createPageable(page, size, "id");
        List<GroupResponse.MyGroupDTO> myGroupDTOS = getMyGroupDTOS(userId, pageable);

        if(myGroupDTOS.isEmpty()){
            throw new CustomException(ExceptionCode.SEARCH_NOT_FOUND);
        }

        return new GroupResponse.FindMyGroupDTO(myGroupDTOS);
    }

    @Transactional
    public void joinGroup(GroupRequest.JoinGroupDTO requestDTO, Long userId, Long groupId){
        // 존재하지 않는 그룹이면 에러 처리
        if(!groupRepository.existsById(groupId)){
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }

        // 이미 가입했거나 신청한 회원이면 에러 처리
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(groupUser -> groupUser.getRole().equals(Role.USER) || groupUser.getRole().equals(Role.ADMIN) || groupUser.getRole().equals(Role.TEMP))
                .ifPresent(groupUser -> {
                    throw new CustomException(ExceptionCode.GROUP_ALREADY_JOIN);
                });

        Group groupRef = entityManager.getReference(Group.class, groupId);
        User userRef = entityManager.getReference(User.class, userId);

        GroupUser groupUser = GroupUser.builder()
                .role(Role.TEMP)
                .user(userRef)
                .group(groupRef)
                .greeting(requestDTO.greeting())
                .build();
        groupUserRepository.save(groupUser);

        // 그룹 참가자 수 증가
        groupRepository.incrementParticipantNumById(groupId);
    }

    @Transactional
    public void withdrawGroup(Long userId, Long groupId){
        // 존재하지 않는 그룹이면 에러 처리
        if(!groupRepository.existsById(groupId)){
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }

        // 가입한 회원이 아니면 에러
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(groupUser -> groupUser.getRole().equals(Role.USER) || groupUser.getRole().equals(Role.ADMIN))
                .orElseThrow(
                        () -> new CustomException(ExceptionCode.GROUP_NOT_MEMBER)
                );

        groupUserRepository.deleteByGroupIdAndUserId(groupId, userId);

        // 그룹 참가자 수 감소
        groupRepository.decrementParticipantNumById(groupId);
    }

    @Transactional
    public void approveJoin(Long userId, Long applicantId, Long groupId){
        // 존재하지 않는 그룹이면 에러
        if(!groupRepository.existsById(groupId)){
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }

        // 권한 체크
        checkAuthority(groupId, userId);

        // 신청한 적이 없거나 이미 가입했는지 체크
        Optional<GroupUser> groupApplicantOP = groupUserRepository.findByGroupIdAndUserId(groupId, applicantId);
        checkAlreadyApplyOrMember(groupApplicantOP);

        groupApplicantOP.get().updateRole(Role.USER);
    }

    @Transactional
    public void rejectJoin(Long userId, Long applicantId, Long groupId){
        // 존재하지 않는 그룹이면 에러
        if(!groupRepository.existsById(groupId)){
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }

        // 권한 체크
        checkAuthority(groupId, userId);

        // 신청한 적이 없거나 이미 가입했는지 체크
        Optional<GroupUser> groupApplicantOP = groupUserRepository.findByGroupIdAndUserId(groupId, applicantId);
        checkAlreadyApplyOrMember(groupApplicantOP);

        groupApplicantOP.get().updateRole(Role.REJECTED);
    }

    @Transactional
    public void likeGroup(Long userId, Long groupId){
        // 존재하지 않는 그룹이면 에러
        if(!groupRepository.existsById(groupId)){
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }

        Group groupRef = entityManager.getReference(Group.class, groupId);
        User userRef = entityManager.getReference(User.class, userId);

        Optional<FavoriteGroup> favoriteGroupOP = favoriteGroupRepository.findByUserIdAndGroupId(userId, groupId);

        // 좋아요가 이미 있다면 삭제, 없다면 추가
        if (favoriteGroupOP.isPresent()) {
            favoriteGroupRepository.delete(favoriteGroupOP.get());
        }
        else {
            FavoriteGroup favoriteGroup = FavoriteGroup.builder()
                    .user(userRef)
                    .group(groupRef)
                    .build();
            favoriteGroupRepository.save(favoriteGroup);
        }
    }

    @Transactional
    public GroupResponse.CreateMeetingDTO createMeeting(GroupRequest.CreateMeetingDTO requestDTO, Long groupId, Long userId){
        // 존재하지 않는 그룹이면 에러
        if(!groupRepository.existsById(groupId)){
            throw new CustomException(ExceptionCode.GROUP_NOT_FOUND);
        }

        // 권한 체크 (메니저급만 생성 가능)
        checkAuthority(groupId, userId);

        Group groupRef = entityManager.getReference(Group.class, groupId);
        User userRef = entityManager.getReference(User.class, userId); // 주최자

        Meeting meeting = Meeting.builder()
                .group(groupRef)
                .user(userRef)
                .name(requestDTO.name())
                .date(requestDTO.date())
                .location(requestDTO.location())
                .cost(requestDTO.cost())
                .maxNum(requestDTO.maxNum())
                .description(requestDTO.description())
                .profileURL(requestDTO.profileURL())
                .build();
        meetingRepository.save(meeting);

        // 주최자를 맴버로 저장
        MeetingUser meetingUser = MeetingUser.builder()
                .meeting(meeting)
                .user(userRef)
                .build();
        meetingUserRepository.save(meetingUser);

        return new GroupResponse.CreateMeetingDTO(meeting.getId());
    }

    @Transactional
    public void updateMeeting(GroupRequest.UpdateMeetingDTO requestDTO, Long groupId, Long meetingId, Long userId){
        // 권한 체크(메니저급만 수정 가능)
        checkAuthority(groupId, userId);

        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new CustomException(ExceptionCode.MEETING_NOT_FOUND)
        );

        meeting.updateMeeting(requestDTO.name(), requestDTO.date(), requestDTO.location(), requestDTO.cost(), requestDTO.maxNum(), requestDTO.description(), requestDTO.profileURL());
    }

    @Transactional
    public void joinMeeting(Long groupId, Long meetingId, Long userId){
        // 존재하지 않는 모임이면 에러 처리
        if(!meetingRepository.existsById(meetingId)){
            throw new CustomException(ExceptionCode.MEETING_NOT_FOUND);
        }

        // 그룹의 맴버가 아니면 에러 처리
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(groupUser -> groupUser.getRole().equals(Role.USER) || groupUser.getRole().equals(Role.ADMIN))
                .orElseThrow( () -> new CustomException(ExceptionCode.GROUP_NOT_MEMBER));

        // 이미 참가중인 모임이면 에러 처리
        if(meetingUserRepository.existsByMeetingIdAndUserId(meetingId, userId)){
            throw new CustomException(ExceptionCode.MEETING_ALREADY_JOIN);
        }

        Meeting meetingRef = entityManager.getReference(Meeting.class, meetingId);
        User userRef = entityManager.getReference(User.class, userId);

        MeetingUser meetingUser = MeetingUser.builder()
                .meeting(meetingRef)
                .user(userRef)
                .build();
        meetingUserRepository.save(meetingUser);

        // 참가자 수 증가
        meetingRepository.incrementParticipantNumById(meetingId);
    }

    @Transactional
    public void withdrawMeeting(Long groupId, Long meetingId, Long userId){
        // 존재하지 않는 모임이면 에러 처리
        if(!meetingRepository.existsById(meetingId)){
            throw new CustomException(ExceptionCode.MEETING_NOT_FOUND);
        }

        // 그룹의 맴버가 아니면 에러 처리
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(groupUser -> groupUser.getRole().equals(Role.USER) || groupUser.getRole().equals(Role.ADMIN))
                .orElseThrow( () -> new CustomException(ExceptionCode.GROUP_NOT_MEMBER));

        // 참가중이 맴버가 아니라면 에러 처리
        if(!meetingUserRepository.existsByMeetingIdAndUserId(meetingId, userId)){
            throw new CustomException(ExceptionCode.MEETING_NOT_MEMBER);
        }

        meetingUserRepository.deleteByMeetingIdAndUserId(meetingId, userId);

        // 참가자 수 감소
        meetingRepository.decrementParticipantNumById(meetingId);
    }

    private List<GroupResponse.RecommendGroupDTO> getRecommendGroupDTOS(Long userId, String region){
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

    private List<GroupResponse.LocalGroupDTO> getLocalGroupDTOS(Long userId, String region, Pageable pageable){
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

    private List<GroupResponse.NewGroupDTO> getNewGroupDTOS(Long userId, Pageable pageable){
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

    private List<GroupResponse.MyGroupDTO> getMyGroupDTOS(Long userId, Pageable pageable){

        List<Group> myGroups = getMyGroups(userId, pageable);

        List<GroupResponse.MyGroupDTO> myGroupDTOS = myGroups.stream()
                .map(group -> new GroupResponse.MyGroupDTO(group.getId(), group.getName(), group.getDescription(),
                        group.getParticipationNum(), group.getCategory(), group.getRegion(), group.getSubRegion(), group.getProfileURL(), group.getLikeNum()))
                .collect(Collectors.toList());

        return myGroupDTOS;
    }

    private List<Group> getMyGroups(Long userId, Pageable pageable){

        Page<GroupUser> groupUsers = groupUserRepository.findByUserId(userId, pageable);
        List<Group> myGroups = groupUsers.getContent().stream()
                .map(GroupUser::getGroup)
                .collect(Collectors.toList());

        return myGroups;
    }

    private Pageable createPageable(int page, int size, String sortProperty) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortProperty));
    }

    private void checkAuthority(Long groupId, Long userId){
        // 권한 체크, 엔티티 사이즈가 크지 않으니 그냥 불러오자
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(groupUser -> groupUser.getRole().equals(Role.ADMIN)) // ADMIN인 경우에만 통과 (ADMIN이 아니면 null이 되어 orElseThrow 실행)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_FORBIDDEN)); // ADMIN이 아니거나 그룹과 관련없는 사람이면 에러 보냄
    }

    private void checkAlreadyApplyOrMember(Optional<GroupUser> groupApplicantOP){

        if(groupApplicantOP.isEmpty()){ // 가입 신청한 적이 없음
            throw new CustomException(ExceptionCode.GROUP_NOT_APPLY);
        } // 이미 승인되어 회원이거나 거절됌
        else if(groupApplicantOP.get().getRole().equals(Role.USER) || groupApplicantOP.get().getRole().equals(Role.ADMIN) || groupApplicantOP.get().getRole().equals(Role.REJECTED)){
            throw new CustomException(ExceptionCode.GROUP_ALREADY_JOIN);
        }
    }
}