package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.GroupRequest;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Group.Group;
import com.hong.ForPaw.domain.Group.GroupUser;
import com.hong.ForPaw.domain.Group.Role;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.repository.GroupRepository;
import com.hong.ForPaw.repository.GroupUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final EntityManager entityManager;

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
    public void updateGroup(GroupRequest.UpdateGroupDTO requestDTO, Long groupId, Long userId){
        // 수정 권한 체크
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .ifPresentOrElse(groupUser -> {
                    if (groupUser.getRole().equals(Role.ADMIN)) {
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
}