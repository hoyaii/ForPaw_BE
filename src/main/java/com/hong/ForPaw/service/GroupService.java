package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.GroupRequest;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Group.Group;
import com.hong.ForPaw.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    @Transactional
    public void createGroup(GroupRequest.CreateGroupDTO requestDTO){
        // 이름 중복 체크
        if(groupRepository.findByName(requestDTO.name()).isPresent()){
            throw new CustomException(ExceptionCode.GROUP_NAME_EXIST);
        }

        // 그룹장 설정

        Group group = Group.builder()
                .name(requestDTO.name())
                .region(requestDTO.region())
                .subRegion(requestDTO.subRegion())
                .description(requestDTO.description())
                .category(requestDTO.category())
                .profileURL(requestDTO.profileURL())
                .build();

        groupRepository.save(group);
    }


}
