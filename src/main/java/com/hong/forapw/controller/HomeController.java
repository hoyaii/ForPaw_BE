package com.hong.forapw.controller;

import com.hong.forapw.controller.dto.HomeResponse;
import com.hong.forapw.core.security.CustomUserDetails;
import com.hong.forapw.core.utils.ApiUtils;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/home")
    public ResponseEntity<?> findHome(@AuthenticationPrincipal CustomUserDetails userDetails) {
        HomeResponse.FindHomeDTO responseDTO = homeService.findHomePageData(getUserIdSafely(userDetails));
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    private Long getUserIdSafely(CustomUserDetails userDetails) {
        return Optional.ofNullable(userDetails)
                .map(CustomUserDetails::getUser)
                .map(User::getId)
                .orElse(null);
    }
}
