package com.hong.forapw.controller;

import com.hong.forapw.controller.dto.GroupRequest;
import com.hong.forapw.controller.dto.GroupResponse;
import com.hong.forapw.core.security.CustomUserDetails;
import com.hong.forapw.core.utils.ApiUtils;
import com.hong.forapw.service.group.GroupService;
import com.hong.forapw.service.group.MeetingService;
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

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class MeetingController {

    private final MeetingService meetingService;
    private final GroupService groupService;
    private static final String SORT_BY_ID = "id";

    @PostMapping("/groups/{groupId}/meetings")
    public ResponseEntity<?> createMeeting(@RequestBody @Valid GroupRequest.CreateMeetingDTO requestDTO, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponse.CreateMeetingDTO responseDTO = meetingService.createMeeting(requestDTO, groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/groups/{groupId}/meetings/{meetingId}")
    public ResponseEntity<?> updateMeeting(@RequestBody @Valid GroupRequest.UpdateMeetingDTO requestDTO, @PathVariable Long groupId, @PathVariable Long meetingId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        meetingService.updateMeeting(requestDTO, groupId, meetingId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/meetings/{meetingId}/join")
    public ResponseEntity<?> joinMeeting(@PathVariable Long groupId, @PathVariable Long meetingId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        meetingService.joinMeeting(groupId, meetingId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/meetings/{meetingId}/withdraw")
    public ResponseEntity<?> withdrawMeeting(@PathVariable Long groupId, @PathVariable Long meetingId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        meetingService.withdrawMeeting(groupId, meetingId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/groups/{groupId}/meetings/{meetingId}")
    public ResponseEntity<?> deleteMeeting(@PathVariable Long groupId, @PathVariable Long meetingId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        meetingService.deleteMeeting(groupId, meetingId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/groups/{groupId}/meetings/{meetingId}")
    public ResponseEntity<?> findMeetingById(@PathVariable Long groupId, @PathVariable Long meetingId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponse.FindMeetingByIdDTO responseDTO = meetingService.findMeetingById(meetingId, groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}/meetings")
    public ResponseEntity<?> findMeetingList(@PathVariable Long groupId,
                                             @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.checkGroupAndIsMember(groupId, userDetails.getUser().getId());
        List<GroupResponse.MeetingDTO> meetingDTOS = meetingService.findMeetings(groupId, pageable);
        GroupResponse.FindMeetingListDTO responseDTO = new GroupResponse.FindMeetingListDTO(meetingDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}
