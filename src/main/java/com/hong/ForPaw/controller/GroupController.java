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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
        List<Long> likedGroupIdList = groupService.getLikedGroupIdList(userId);
        GroupResponse.FindAllGroupListDTO responseDTO = groupService.findGroupList(userId, likedGroupIdList);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/local")
    public ResponseEntity<?> findLocalGroupList(@RequestParam("province") Province province, @RequestParam("district") District district, Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        Long userId = getUserIdSafely(userDetails);
        List<Long> likedGroupIdList = groupService.getLikedGroupIdList(userId);
        List<GroupResponse.LocalGroupDTO> localGroupDTOS = groupService.findLocalGroupList(userId, province, district, likedGroupIdList, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, new GroupResponse.FindLocalGroupListDTO(localGroupDTOS)));
    }

    @GetMapping("/groups/new")
    public ResponseEntity<?> findNewGroupList(@RequestParam(value = "province", required = false) Province province, Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        List<GroupResponse.NewGroupDTO> newGroupDTOS = groupService.findNewGroupList(userDetails.getUser().getId(), province, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, new GroupResponse.FindNewGroupListDTO(newGroupDTOS)));
    }

    @GetMapping("/groups/my")
    public ResponseEntity<?> findMyGroupList(@RequestParam("page") Integer page, @AuthenticationPrincipal CustomUserDetails userDetails){
        GroupResponse.FindMyGroupListDTO responseDTO = groupService.findMyGroupList(userDetails.getUser().getId(), page);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}/detail")
    public ResponseEntity<?> findGroupDetailById(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails){
        GroupResponse.FindGroupDetailByIdDTO responseDTO = groupService.findGroupDetailById(userDetails.getUser().getId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}/notices")
    public ResponseEntity<?> findNoticeList(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("page") Integer page){
        GroupResponse.FindNoticeListDTO responseDTO = groupService.findNoticeList(userDetails.getUser().getId(), groupId, page);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}/meetings")
    public ResponseEntity<?> findMeetingList(@PathVariable Long groupId, @RequestParam("page") Integer page, @AuthenticationPrincipal CustomUserDetails userDetails){
        GroupResponse.FindMeetingListDTO responseDTO = groupService.findMeetingList(userDetails.getUser().getId(), groupId, page);
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

    @PostMapping("/groups/{groupID}/join/approve")
    public ResponseEntity<?> approveJoin(@RequestBody @Valid GroupRequest.ApproveJoinDTO requestDTO, @PathVariable Long groupID, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.approveJoin(userDetails.getUser().getId(), requestDTO.id(), groupID);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupID}/join/reject")
    public ResponseEntity<?> rejectJoin(@RequestBody @Valid GroupRequest.RejectJoinDTO requestDTO, @PathVariable Long groupID, @AuthenticationPrincipal CustomUserDetails userDetails){
        groupService.rejectJoin(userDetails.getUser().getId(), requestDTO.id(), groupID);
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