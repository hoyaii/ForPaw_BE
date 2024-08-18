package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.AuthenticationRequest;
import com.hong.ForPaw.controller.DTO.AuthenticationResponse;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.domain.Apply.ApplyStatus;
import com.hong.ForPaw.domain.Inquiry.InquiryStatus;
import com.hong.ForPaw.domain.Report.ReportStatus;
import com.hong.ForPaw.service.AuthenticationService;

import com.hong.ForPaw.service.UserService;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private static final String SORT_BY_ID = "id";

    @GetMapping("/admin/dashboard")
    public ResponseEntity<?> findDashboardStats(@AuthenticationPrincipal CustomUserDetails userDetails){
        AuthenticationResponse.FindDashboardStatsDTO responseDTO = authenticationService.findDashboardStats(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/admin/user")
    public ResponseEntity<?> findUserList(@PageableDefault(sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        AuthenticationResponse.FindUserListDTO responseDTO = authenticationService.findUserList(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/admin/user/role")
    public ResponseEntity<?> changeUserRole(@RequestBody @Valid AuthenticationRequest.ChangeUserRoleDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){
        authenticationService.changeUserRole(requestDTO, userDetails.getUser().getId(), userDetails.getUser().getRole());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/admin/user/suspend")
    public ResponseEntity<?> suspendUser(@RequestBody @Valid AuthenticationRequest.SuspendUserDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){
        authenticationService.suspendUser(requestDTO, userDetails.getUser().getId(), userDetails.getUser().getRole());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,null));
    }

    @PostMapping("/admin/user/unsuspend")
    public ResponseEntity<?> unSuspendUser(@RequestBody @Valid AuthenticationResponse.UnSuspendUserDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){
        authenticationService.unSuspendUser(requestDTO.userId(), userDetails.getUser().getId(), userDetails.getUser().getRole());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,null));
    }

    @DeleteMapping("/admin/user")
    public ResponseEntity<?> withdrawUser(@RequestBody @Valid AuthenticationResponse.WithdrawUserDTO requestDTO ,@AuthenticationPrincipal CustomUserDetails userDetails){
        authenticationService.checkAdminAuthority(userDetails.getUser().getId());
        userService.withdrawMember(requestDTO.userId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,null));
    }

    @GetMapping("/admin/adoption")
    public ResponseEntity<?> findApplyList(@RequestParam(required = false) ApplyStatus status,
                                           @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        AuthenticationResponse.FindApplyListDTO responseDTO = authenticationService.findApplyList(userDetails.getUser().getId(), status, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/admin/adoption")
    public ResponseEntity<?> changeApplyStatus(@RequestBody @Valid AuthenticationRequest.ChangeApplyStatusDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){
        authenticationService.changeApplyStatus(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,null));
    }

    @GetMapping("/admin/reports")
    public ResponseEntity<?> findReportList(@RequestParam(required = false) ReportStatus status,
                                            @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        AuthenticationResponse.FindReportListDTO responseDTO = authenticationService.findReportList(userDetails.getUser().getId(), status, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/admin/reports")
    public ResponseEntity<?> processReport(@RequestBody @Valid AuthenticationRequest.ProcessReportDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){
        authenticationService.processReport(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/admin/supports")
    public ResponseEntity<?> findSupportList(@RequestParam(required = false) InquiryStatus status,
                                             @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        AuthenticationResponse.FindSupportListDTO responseDTO = authenticationService.findSupportList(userDetails.getUser().getId(), status, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/admin/supports/{inquiryId}")
    public ResponseEntity<?> findSupportById(@PathVariable Long inquiryId, @AuthenticationPrincipal CustomUserDetails userDetails){
        AuthenticationResponse.FindSupportByIdDTO responseDTO = authenticationService.findSupportById(userDetails.getUser().getId(), inquiryId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/admin/supports/{inquiryId}/answer")
    public ResponseEntity<?> answerInquiry(@RequestBody @Valid AuthenticationRequest.AnswerInquiryDTO requestDTO, @PathVariable Long inquiryId, @AuthenticationPrincipal CustomUserDetails userDetails){
        AuthenticationResponse.AnswerInquiryDTO responseDTO = authenticationService.answerInquiry(requestDTO, userDetails.getUser().getId(), inquiryId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}
