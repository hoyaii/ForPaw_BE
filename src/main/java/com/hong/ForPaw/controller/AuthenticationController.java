package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.AuthenticationResponse;
import com.hong.ForPaw.controller.DTO.AuthenticationResponse.ApplyDTO;
import com.hong.ForPaw.controller.DTO.AuthenticationResponse.UserRoleDTO;
import com.hong.ForPaw.controller.DTO.AuthenticationResponse.findUserListDTO;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.domain.Apply.ApplyStatus;
import com.hong.ForPaw.service.AuthenticationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @GetMapping("/admin/dashboard")
    public ResponseEntity<?> findDashboardStats(@AuthenticationPrincipal CustomUserDetails userDetails){
        AuthenticationResponse.FindDashboardStatsDTO responseDTO = authenticationService.findDashboardStats(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/admin/user")
    public ResponseEntity<?> getUsers(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(defaultValue = "0") int page) {
        findUserListDTO responseDTO = authenticationService.findUserList(userDetails.getUser().getId(),
            userDetails.getUser().getRole(), page);

        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,responseDTO));
    }

    @PatchMapping("/admin/user/role")
    public ResponseEntity<?> changeUserRole(@AuthenticationPrincipal CustomUserDetails userDetails,
        AuthenticationResponse.UserRoleDTO userRoleDTO){
        UserRoleDTO responseDTO = authenticationService.changeUserRole(
            userDetails.getUser().getId(), userRoleDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,responseDTO));
    }

    @PostMapping("/admin/user/suspend")
    public ResponseEntity<?> suspendUser(@AuthenticationPrincipal CustomUserDetails customUserDetails,
        AuthenticationResponse.UserBanDTO userBanDTO){
        authenticationService.suspendUser(customUserDetails.getUser().getId(),userBanDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,null));
    }

    @PostMapping("/admin/user/unsuspend")
    public ResponseEntity<?> unSuspendUser(@AuthenticationPrincipal CustomUserDetails customUserDetails,
        Long userId){
        authenticationService.unSuspendUser(customUserDetails.getUser().getId(),userId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,null));
    }

    @DeleteMapping("/admin/user")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal CustomUserDetails customUserDetails,
        Long userId){
        authenticationService.deleteUser(customUserDetails.getUser().getId(),userId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,null));
    }

    @GetMapping("/admin/adoption")
    public ResponseEntity<?> getApply(@AuthenticationPrincipal CustomUserDetails customUserDetails,
        @RequestParam(required = false)ApplyStatus applyStatus,
        @RequestParam(defaultValue = "0") int page){
        List<ApplyDTO> responseDTO = authenticationService.getApplyList(customUserDetails.getUser().getId(),
            applyStatus, page);

        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,responseDTO));
    }

    @PatchMapping("/admin/adoption")
    public ResponseEntity<?> changeApplyStatus(@AuthenticationPrincipal CustomUserDetails customUserDetails,
       @RequestParam("id")Long id, @RequestParam("status")ApplyStatus applyStatus){
        authenticationService.changeApplyStatus(customUserDetails.getUser().getId(),id,applyStatus);

        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,null));
    }



}
