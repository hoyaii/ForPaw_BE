package com.hong.forapw.domain.meeting;

import com.hong.forapw.security.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import com.hong.forapw.domain.group.service.GroupService;
import com.hong.forapw.domain.meeting.model.MeetingRequest;
import com.hong.forapw.domain.meeting.model.MeetingResponse;
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
    public ResponseEntity<?> createMeeting(@RequestBody @Valid MeetingRequest.CreateMeetingDTO requestDTO, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        MeetingResponse.CreateMeetingDTO responseDTO = meetingService.createMeeting(requestDTO, groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/groups/{groupId}/meetings/{meetingId}")
    public ResponseEntity<?> updateMeeting(@RequestBody @Valid MeetingRequest.UpdateMeetingDTO requestDTO, @PathVariable Long groupId, @PathVariable Long meetingId, @AuthenticationPrincipal CustomUserDetails userDetails) {
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
        MeetingResponse.FindMeetingByIdDTO responseDTO = meetingService.findMeetingById(meetingId, groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}/meetings")
    public ResponseEntity<?> findMeetingList(@PathVariable Long groupId,
                                             @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.checkGroupAndIsMember(groupId, userDetails.getUser().getId());
        List<MeetingResponse.MeetingDTO> meetingDTOS = meetingService.findMeetings(groupId, pageable);
        MeetingResponse.FindMeetingListDTO responseDTO = new MeetingResponse.FindMeetingListDTO(meetingDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}
