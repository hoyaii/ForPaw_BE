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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @Value("${kakao.oauth.redirect.uri}")
    private String redirectUrl;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody @Valid UserRequest.LoginDTO requestDTO, HttpServletRequest request) throws MessagingException {
        Map<String, String> tokens = userService.login(requestDTO, request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, ResponseCookie.from("refreshToken", tokens.get("refreshToken"))
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .sameSite("Lax")
                        .maxAge(JWTProvider.REFRESH_EXP_SEC)
                        .build().toString())
                .body(ApiUtils.success(HttpStatus.OK, new UserResponse.LoginDTO(tokens.get("accessToken"))));
    }

    @GetMapping("/auth/login/kakao")
    public void kakaoLogin(@RequestParam String code, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> tokenOrEmail = userService.kakaoLogin(code, request);

        // 가입된 계정이 아님
        if (tokenOrEmail.get("email") != null) {
            String redirectUrl = "http://localhost:3000/login?email=" + URLEncoder.encode(tokenOrEmail.get("email"), "UTF-8");
            response.sendRedirect(redirectUrl);
            return;
        }

        // 가입된 계정이면, JWT 토큰 반환 및 리다이렉트
        String accessToken = tokenOrEmail.get("accessToken");
        String refreshToken = tokenOrEmail.get("refreshToken");
        String redirectUrl = "http://localhost:3000/login?accessToken=" + URLEncoder.encode(accessToken, "UTF-8");

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(JWTProvider.REFRESH_EXP_SEC)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/auth/login/google")
    public ResponseEntity<?> googleLogin(@RequestParam String code, HttpServletRequest request){
        Map<String, String> tokenOrEmail = userService.googleLogin(code, request);

        // 가입된 계정이 아님
        if(tokenOrEmail.get("email") != null){
            return ResponseEntity.ok().body((ApiUtils.success(HttpStatus.OK, new UserResponse.GoogleLoginDTO("", tokenOrEmail.get("email")))));
        }

        // 가입된 계정이면, jwt 토큰 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, ResponseCookie.from("refreshToken", tokenOrEmail.get("refreshToken"))
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("None")
                        .maxAge(JWTProvider.REFRESH_EXP_MILLI)
                        .build().toString())
                .body(ApiUtils.success(HttpStatus.OK, new UserResponse.GoogleLoginDTO(tokenOrEmail.get("accessToken"), "")));
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
                .header(HttpHeaders.SET_COOKIE, ResponseCookie.from("refreshToken", tokens.get("refreshToken"))
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .sameSite("Lax")
                        .maxAge(JWTProvider.REFRESH_EXP_SEC)
                        .domain("localhost")
                        .build().toString())
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
}