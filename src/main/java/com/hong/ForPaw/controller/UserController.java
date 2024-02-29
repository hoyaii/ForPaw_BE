package com.hong.ForPaw.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hong.ForPaw.controller.DTO.UserRequest;
import com.hong.ForPaw.controller.DTO.UserResponse;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.security.JWTProvider;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody UserRequest.LoginDTO requestDTO) {

        UserResponse.JwtTokenDTO responseDTO = userService.login(requestDTO);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, ResponseCookie.from("refreshToken", responseDTO.refreshToken())
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("None")
                        .maxAge(JWTProvider.REFRESH_EXP)
                        .build().toString())
                .body(ApiUtils.success(HttpStatus.OK, new UserResponse.AccessTokenDTO(responseDTO.accessToken())));
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> join(@RequestBody UserRequest.JoinDTO requestDTO){

        userService.join(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, null));
    }

    @PostMapping("/accounts/check")
    public ResponseEntity<?> checkAndSendCode(@RequestBody UserRequest.EmailDTO requestDTO){

        userService.checkAndSendCode(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/check/verify")
    public ResponseEntity<?> verifyRegisterCode(@RequestBody UserRequest.VerifyCodeDTO requestDTO){

        userService.verifyCode(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/recovery")
    public ResponseEntity<?> sendRecoveryCode(@RequestBody UserRequest.EmailDTO requestDTO){

        userService.sendRecoveryCode(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/recovery/verify")
    public ResponseEntity<?> verifyAndSendPassword(@RequestBody UserRequest.VerifyCodeDTO requestDTO){

        userService.verifyAndSendPassword(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/password/verify")
    public ResponseEntity<?> verifyPassword(@RequestBody UserRequest.CurPasswordDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){

        userService.verifyPassword(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }


    @PatchMapping("/accounts/password")
    public ResponseEntity<?> updatePassword(@RequestBody UserRequest.UpdatePasswordDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){

        userService.updatePassword(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/accounts/profile")
    public ResponseEntity<?> findProfile(@AuthenticationPrincipal CustomUserDetails userDetails){

        UserResponse.ProfileDTO responseDTO = userService.findProfile(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/accounts/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UserRequest.UpdateProfileDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){

        userService.updateProfile(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PatchMapping("/auth/access")
    public ResponseEntity<?> updateAccessToken(@RequestBody UserRequest.RefreshTokenDTO requestDTO){

        UserResponse.AccessTokenDTO responseDTO = userService.updateAccessToken(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}