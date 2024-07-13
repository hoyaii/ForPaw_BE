package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.AuthenticationResponse;
import com.hong.ForPaw.controller.DTO.AuthenticationResponse.UserBanDTO;
import com.hong.ForPaw.controller.DTO.AuthenticationResponse.UserRoleDTO;
import com.hong.ForPaw.controller.DTO.AuthenticationResponse.findUserList;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.security.JWTProvider;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
        findUserList userList = authenticationService.findUserList(userDetails.getUser().getId(),
            userDetails.getUser().getRole(), page);

        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,userList));
    }

    @PatchMapping("/admin/user/role")
    public ResponseEntity<?> ChangeUserRole(@AuthenticationPrincipal CustomUserDetails userDetails,
        AuthenticationResponse.UserRoleDTO userRoleDTO){
        UserRoleDTO userRole = authenticationService.changeUserRole(
            userDetails.getUser().getId(), userRoleDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,userRole));
    }

    @PostMapping("/admin/user/suspend")
    public ResponseEntity<?> BanUser(@AuthenticationPrincipal CustomUserDetails customUserDetails,
        AuthenticationResponse.UserBanDTO userBanDTO){
        authenticationService.BanUser(customUserDetails.getUser().getId(),userBanDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,null));
    }


}
