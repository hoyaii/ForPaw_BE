package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.UserRequest;
import com.hong.ForPaw.controller.DTO.UserResponse;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.security.JWTProvider;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.service.UserService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @Value("${social.join.redirect.uri}")
    private String REDIRECT_JOIN_URI;

    @Value("${social.home.redirect.uri}")
    private String REDIRECT_HOME_URI;
    private static final String AUTH_KAKAO = "KAKAO";
    private static final String AUTH_GOOGLE = "GOOGLE";

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody @Valid UserRequest.LoginDTO requestDTO, HttpServletRequest request) throws MessagingException {
        Map<String, String> tokens = userService.login(requestDTO, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(tokens.get("refreshToken")))
                .body(ApiUtils.success(HttpStatus.OK, new UserResponse.LoginDTO(tokens.get("accessToken"))));
    }

    @GetMapping("/auth/login/kakao")
    public void kakaoLogin(@RequestParam String code, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> tokenOrEmail = userService.kakaoLogin(code, request);
        handleAuthenticationRedirect(tokenOrEmail, AUTH_KAKAO, response);
    }

    @GetMapping("/auth/login/google")
    public void googleLogin(@RequestParam String code, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> tokenOrEmail = userService.googleLogin(code, request);
        handleAuthenticationRedirect(tokenOrEmail, AUTH_GOOGLE, response);
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> join(@RequestBody @Valid UserRequest.JoinDTO requestDTO){
        userService.join(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, null));
    }

    @PostMapping("/accounts/social")
    public ResponseEntity<?> socialJoin(@RequestBody @Valid UserRequest.SocialJoinDTO requestDTO){
        userService.socialJoin(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, null));
    }

    @PostMapping("/accounts/check/email")
    public ResponseEntity<?> checkEmailAndSendCode(@RequestBody @Valid UserRequest.EmailDTO requestDTO) throws MessagingException {
        userService.checkEmailExist(requestDTO);
        userService.sendCodeByEmail(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/resend/code")
    public ResponseEntity<?> resendCode(@RequestBody @Valid UserRequest.EmailDTO requestDTO) throws MessagingException {
        userService.sendCodeByEmail(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/check/nick")
    public ResponseEntity<?> checkNick(@RequestBody @Valid UserRequest.CheckNickDTO requestDTO){
        userService.checkNick(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/check/email/verify")
    public ResponseEntity<?> verifyRegisterCode(@RequestBody @Valid UserRequest.VerifyCodeDTO requestDTO){
        userService.verifyRegisterCode(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/recovery")
    public ResponseEntity<?> sendRecoveryCode(@RequestBody @Valid UserRequest.EmailDTO requestDTO) throws MessagingException {
        userService.checkAccountExist(requestDTO);
        userService.sendCodeByEmail(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/recovery/verify")
    public ResponseEntity<?> verifyRecoveryCode(@RequestBody @Valid UserRequest.VerifyCodeDTO requestDTO){
        userService.verifyRecoveryCode(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/recovery/reset")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid UserRequest.ResetPasswordDTO requestDTO){
        userService.resetPassword(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/password/verify")
    public ResponseEntity<?> verifyPassword(@RequestBody @Valid UserRequest.CurPasswordDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){
        UserResponse.VerifyPasswordDTO responseDTO = userService.verifyPassword(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/accounts/password")
    public ResponseEntity<?> updatePassword(@RequestBody @Valid UserRequest.UpdatePasswordDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){
        userService.updatePassword(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/accounts/profile")
    public ResponseEntity<?> findProfile(@AuthenticationPrincipal CustomUserDetails userDetails){
        UserResponse.ProfileDTO responseDTO = userService.findProfile(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/accounts/profile")
    public ResponseEntity<?> updateProfile(@RequestBody @Valid UserRequest.UpdateProfileDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){
        userService.updateProfile(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PatchMapping("/auth/access")
    public ResponseEntity<?> updateAccessToken(@CookieValue String refreshToken){
        Map<String, String> tokens = userService.updateAccessToken(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(tokens.get("refreshToken")))
                .body(ApiUtils.success(HttpStatus.OK, new UserResponse.AccessTokenDTO(tokens.get("accessToken"))));
    }

    // 관리자 페이지용
    @PatchMapping("/accounts/role")
    public ResponseEntity<?> updateRole(@RequestBody @Valid UserRequest.UpdateRoleDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){
        userService.updateRole(requestDTO, userDetails.getUser().getRole());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/accounts/withdraw")
    public ResponseEntity<?> withdrawMember(@AuthenticationPrincipal CustomUserDetails userDetails){
        userService.withdrawMember(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/supports")
    public ResponseEntity<?> submitInquiry(@RequestBody @Valid UserRequest.SubmitInquiry requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails){
        UserResponse.SubmitInquiryDTO responseDTO = userService.submitInquiry(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/supports/{inquiryId}")
    public ResponseEntity<?> updateInquiry(@RequestBody @Valid UserRequest.UpdateInquiry requestDTO, @PathVariable Long inquiryId, @AuthenticationPrincipal CustomUserDetails userDetails){
        userService.updateInquiry(requestDTO, inquiryId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/supports")
    public ResponseEntity<?> findInquiryList(@AuthenticationPrincipal CustomUserDetails userDetails){
        UserResponse.FindInquiryListDTO responseDTO = userService.findInquiryList(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/supports/{inquiryId}")
    public ResponseEntity<?> findInquiryById(@PathVariable Long inquiryId, @AuthenticationPrincipal CustomUserDetails userDetails){
        UserResponse.FindInquiryByIdDTO responseDTO = userService.findInquiryById(userDetails.getUser().getId(), inquiryId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/validate/accessToken")
    public ResponseEntity<?> validateAccessToken(@CookieValue String accessToken){
        UserResponse.ValidateAccessTokenDTO responseDTO = userService.validateAccessToken(accessToken);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/communityStats")
    public ResponseEntity<?> findCommunityStats(@AuthenticationPrincipal CustomUserDetails userDetails){
        UserResponse.FindCommunityRecord responseDTO = userService.findCommunityStats(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    private void handleAuthenticationRedirect(Map<String, String> tokenOrEmail, String authProvider, HttpServletResponse response) throws IOException {
        String redirectUri;
        if(tokenOrEmail.get("email") != null) {
            redirectUri = UriComponentsBuilder.fromUriString(REDIRECT_JOIN_URI)
                    .queryParam("email", URLEncoder.encode(tokenOrEmail.get("email"), StandardCharsets.UTF_8))
                    .queryParam("authProvider", URLEncoder.encode(authProvider, StandardCharsets.UTF_8))
                    .build()
                    .toUriString();
        } else {
            response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(tokenOrEmail.get("refreshToken")));
            redirectUri = UriComponentsBuilder.fromUriString(REDIRECT_HOME_URI)
                    .queryParam("accessToken", URLEncoder.encode(tokenOrEmail.get("accessToken"), StandardCharsets.UTF_8))
                    .queryParam("authProvider", URLEncoder.encode(authProvider, StandardCharsets.UTF_8))
                    .build()
                    .toUriString();
        }
        response.sendRedirect(redirectUri);
    }

    private String createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(JWTProvider.REFRESH_EXP_SEC)
                .build().toString();
    }
}