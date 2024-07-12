package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.AuthenticationResponse;
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

    @GetMapping("/user")
    public ResponseEntity<?> getUsers(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(defaultValue = "0") int page) {
        findUserList userList = authenticationService.findUserList(userDetails.getUser().getId(),
            userDetails.getUser().getRole(), page);

        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK,userList));
    }
}
