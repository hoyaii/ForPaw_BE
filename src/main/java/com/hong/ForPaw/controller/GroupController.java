package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.GroupRequest;
import com.hong.ForPaw.controller.DTO.GroupResponse;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.domain.District;
import com.hong.ForPaw.domain.Province;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(@RequestBody @Valid GroupRequest.CreateGroupDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){
        GroupResponse.CreateGroupDTO responseDTO = groupService.createGroup(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<?> findGroupById(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails){
        GroupResponse.FindGroupByIdDTO responseDTO = groupService.findGroupById(groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/groups/{groupId}")
    public ResponseEntity<?> updateGroup(@RequestBody @Valid GroupRequest.UpdateGroupDTO requestDTO, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.updateGroup(requestDTO, groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/groups")
    public ResponseEntity<?> findGroupList(@AuthenticationPrincipal CustomUserDetails userDetails){
        Long userId = getUserIdSafely(userDetails);
        GroupResponse.FindAllGroupListDTO responseDTO = groupService.findGroupList(userId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/local")
    public ResponseEntity<?> findLocalGroupList(@RequestParam Province province, @RequestParam District district, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        Long userId = getUserIdSafely(userDetails);
        List<GroupResponse.LocalGroupDTO> localGroupDTOS = groupService.findLocalGroupList(userId, province, district, Collections.emptyList(), pageable);
        GroupResponse.FindLocalGroupListDTO responseDTO = new GroupResponse.FindLocalGroupListDTO(localGroupDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/new")
    public ResponseEntity<?> findNewGroupList(@RequestParam(value = "province", required = false) Province province, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        List<GroupResponse.NewGroupDTO> newGroupDTOS = groupService.findNewGroupList(userDetails.getUser().getId(), province, pageable);
        GroupResponse.FindNewGroupListDTO responseDTO = new GroupResponse.FindNewGroupListDTO(newGroupDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/my")
    public ResponseEntity<?> findMyGroupList(@PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        Long userId = getUserIdSafely(userDetails);
        List<GroupResponse.MyGroupDTO> myGroupDTOS = groupService.findMyGroupList(userId, Collections.emptyList(), pageable);
        GroupResponse.FindMyGroupListDTO responseDTO = new GroupResponse.FindMyGroupListDTO(myGroupDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}/detail")
    public ResponseEntity<?> findGroupDetailById(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails){
        Long userId = getUserIdSafely(userDetails);
        GroupResponse.FindGroupDetailByIdDTO responseDTO = groupService.findGroupDetailById(userId, groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}/notices")
    public ResponseEntity<?> findNoticeList(@PathVariable Long groupId, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.checkGroupAndMember(groupId, userDetails.getUser().getId());
        List<GroupResponse.NoticeDTO> noticeDTOS = groupService.findNoticeList(userDetails.getUser().getId(), groupId, pageable);
        GroupResponse.FindNoticeListDTO responseDTO = new GroupResponse.FindNoticeListDTO(noticeDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}/meetings")
    public ResponseEntity<?> findMeetingList(@PathVariable Long groupId, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.checkGroupAndMember(groupId, userDetails.getUser().getId());
        List<GroupResponse.MeetingDTO> meetingDTOS = groupService.findMeetingList(groupId, pageable);
        GroupResponse.FindMeetingListDTO responseDTO = new GroupResponse.FindMeetingListDTO(meetingDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}/meetings/{meetingId}")
    public ResponseEntity<?> findMeetingById(@PathVariable Long groupId, @PathVariable Long meetingId, @AuthenticationPrincipal CustomUserDetails userDetails){
        GroupResponse.MeetingDTO responseDTO = groupService.findMeetingById(meetingId, groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/groups/{groupId}/join")
    public ResponseEntity<?> joinGroup(@RequestBody @Valid GroupRequest.JoinGroupDTO requestDTO, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.joinGroup(requestDTO, userDetails.getUser().getId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/withdraw")
    public ResponseEntity<?> withdrawGroup(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.withdrawGroup(userDetails.getUser().getId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/groups/{groupId}/join")
    public ResponseEntity<?> findApplicantList(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails){
        GroupResponse.FindApplicantListDTO responseDTO = groupService.findApplicantList(userDetails.getUser().getId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/groups/{groupID}/join/approve")
    public ResponseEntity<?> approveJoin(@RequestBody @Valid GroupRequest.ApproveJoinDTO requestDTO, @PathVariable Long groupID, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.approveJoin(userDetails.getUser().getId(), requestDTO.applicantId(), groupID);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupID}/join/reject")
    public ResponseEntity<?> rejectJoin(@RequestBody @Valid GroupRequest.RejectJoinDTO requestDTO, @PathVariable Long groupID, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.rejectJoin(userDetails.getUser().getId(), requestDTO.applicantId(), groupID);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/notices")
    public ResponseEntity<?> createNotice(@RequestBody @Valid GroupRequest.CreateNoticeDTO requestDTO, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails){
        GroupResponse.CreateNoticeDTO responseDTO = groupService.createNotice(requestDTO, userDetails.getUser().getId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/groups/{groupId}/like")
    public ResponseEntity<?> likeGroup(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.likeGroup(userDetails.getUser().getId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.deleteGroup(groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PatchMapping("/groups/{groupId}/role")
    public ResponseEntity<?> updateUserRole(@RequestBody @Valid GroupRequest.UpdateUserRoleDTO requestDTO, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.updateUserRole(requestDTO, groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/meetings")
    public ResponseEntity<?> createMeeting(@RequestBody @Valid GroupRequest.CreateMeetingDTO requestDTO, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails){
        GroupResponse.CreateMeetingDTO responseDTO = groupService.createMeeting(requestDTO, groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/groups/{groupId}/meetings/{meetingId}")
    public ResponseEntity<?> updateMeeting(@RequestBody @Valid GroupRequest.UpdateMeetingDTO requestDTO, @PathVariable Long groupId, @PathVariable Long meetingId, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.updateMeeting(requestDTO, groupId, meetingId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/meetings/{meetingId}/join")
    public ResponseEntity<?> joinMeeting(@PathVariable Long groupId, @PathVariable Long meetingId, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.joinMeeting(groupId, meetingId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/meetings/{meetingId}/withdraw")
    public ResponseEntity<?> withdrawMeeting(@PathVariable Long groupId, @PathVariable Long meetingId, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.withdrawMeeting(groupId, meetingId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/groups/{groupId}/meetings/{meetingId}")
    public ResponseEntity<?> deleteMeeting(@PathVariable Long groupId, @PathVariable Long meetingId, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.deleteMeeting(groupId, meetingId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    // 로그인 되지 않았을 때, NullException 방지하면서 userId를 null로 처리하기 위한 메서드
    private Long getUserIdSafely(CustomUserDetails userDetails) {
        return Optional.ofNullable(userDetails)
                .map(CustomUserDetails::getUser)
                .map(User::getId)
                .orElse(null);
    }
}