package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.GroupRequest;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(@RequestBody GroupRequest.CreateGroupDTO requestDTO){

        groupService.createGroup(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}