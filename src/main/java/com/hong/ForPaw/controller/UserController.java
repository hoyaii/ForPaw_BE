package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.UserRequest;
import com.hong.ForPaw.controller.DTO.UserResponse;
import com.hong.ForPaw.core.security.JWTProvider;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserRequest.LoginDTO requestDTO) {
        UserResponse.TokenDTO tokenDTO = userService.login(requestDTO);
        ResponseCookie responseCookie = ResponseCookie.from("refreshToken", tokenDTO.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(JWTProvider.REFRESH_EXP)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(new UserResponse.LoginDTO(tokenDTO.accessToken()));
    }

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody UserRequest.JoinDTO requestDTO){
        userService.join(requestDTO);

        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, null));
    }
}