package com.hong.forapw.domain.user;

import com.hong.forapw.domain.user.model.LoginResult;
import com.hong.forapw.domain.user.model.TokenResponse;
import com.hong.forapw.domain.user.model.UserRequest;
import com.hong.forapw.domain.user.model.UserResponse;
import com.hong.forapw.integration.oauth.OAuthService;
import com.hong.forapw.security.CustomUserDetails;
import com.hong.forapw.common.utils.JwtUtils;
import com.hong.forapw.common.utils.ApiUtils;
import com.hong.forapw.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.hong.forapw.common.GlobalConstants.ACCESS_TOKEN_KEY;
import static com.hong.forapw.common.GlobalConstants.REFRESH_TOKEN_KEY;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final OAuthService oAuthService;
    private final JwtUtils jwtUtils;

    private static final String AUTH_KAKAO = "KAKAO";
    private static final String AUTH_GOOGLE = "GOOGLE";
    private static final String CODE_TYPE_JOIN = "join";
    private static final String CODE_TYPE_WITHDRAW = "withdraw";
    private static final String CODE_TYPE_RECOVERY = "recovery";

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody @Valid UserRequest.LoginDTO requestDTO, HttpServletRequest request) {
        LoginResult loginResult = userService.login(requestDTO, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtUtils.refreshTokenCookie(loginResult.refreshToken()))
                .body(ApiUtils.success(HttpStatus.OK, new UserResponse.LoginDTO(loginResult.accessToken())));
    }

    @GetMapping("/auth/login/kakao")
    public void loginWithKakao(@RequestParam String code, HttpServletRequest request, HttpServletResponse response) {
        LoginResult loginResult = oAuthService.loginWithKakao(code, request);
        oAuthService.redirectAfterOAuthLogin(loginResult, AUTH_KAKAO, response);
    }

    @GetMapping("/auth/login/google")
    public void loginWithGoogle(@RequestParam String code, HttpServletRequest request, HttpServletResponse response) {
        LoginResult loginResult = oAuthService.loginWithGoogle(code, request);
        oAuthService.redirectAfterOAuthLogin(loginResult, AUTH_GOOGLE, response);
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> join(@RequestBody @Valid UserRequest.JoinDTO requestDTO) {
        userService.join(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, null));
    }

    @PostMapping("/accounts/social")
    public ResponseEntity<?> socialJoin(@RequestBody @Valid UserRequest.SocialJoinDTO requestDTO) {
        userService.socialJoin(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, null));
    }

    @PostMapping("/accounts/check/email")
    public ResponseEntity<?> checkEmailAndSendCode(@RequestBody @Valid UserRequest.EmailDTO requestDTO) {
        UserResponse.CheckAccountExistDTO responseDTO = userService.checkAccountExist(requestDTO.email());
        if (responseDTO.isValid()) userService.sendCodeByEmail(requestDTO.email(), CODE_TYPE_JOIN);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/accounts/verify/code")
    public ResponseEntity<?> verifyCode(@RequestBody @Valid UserRequest.VerifyCodeDTO requestDTO, @RequestParam String codeType) {
        UserResponse.VerifyEmailCodeDTO responseDTO = userService.verifyCode(requestDTO, codeType);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/accounts/resend/code")
    public ResponseEntity<?> resendCode(@RequestBody @Valid UserRequest.EmailDTO requestDTO, @RequestParam String codeType) {
        userService.sendCodeByEmail(requestDTO.email(), codeType);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/check/nick")
    public ResponseEntity<?> checkNickname(@RequestBody @Valid UserRequest.CheckNickDTO requestDTO) {
        UserResponse.CheckNickNameDTO responseDTO = userService.checkNickName(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/accounts/withdraw/code")
    public ResponseEntity<?> sendCodeForWithdraw(@RequestBody @Valid UserRequest.EmailDTO requestDTO) {
        UserResponse.CheckAccountExistDTO responseDTO = userService.checkAccountExist(requestDTO.email());
        if (responseDTO.isValid()) userService.sendCodeByEmail(requestDTO.email(), CODE_TYPE_WITHDRAW);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/accounts/recovery/code")
    public ResponseEntity<?> sendCodeForRecovery(@RequestBody @Valid UserRequest.EmailDTO requestDTO) {
        UserResponse.CheckLocalAccountExistDTO responseDTO = userService.checkLocalAccountExist(requestDTO);
        if (responseDTO.isValid()) userService.sendCodeByEmail(requestDTO.email(), CODE_TYPE_RECOVERY);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/accounts/recovery/reset")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid UserRequest.ResetPasswordDTO requestDTO) {
        userService.resetPassword(requestDTO);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/accounts/password/verify")
    public ResponseEntity<?> verifyPassword(@RequestBody @Valid UserRequest.CurPasswordDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse.VerifyPasswordDTO responseDTO = userService.verifyPassword(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/accounts/password")
    public ResponseEntity<?> updatePassword(@RequestBody @Valid UserRequest.UpdatePasswordDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.updatePassword(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/accounts/profile")
    public ResponseEntity<?> findProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse.ProfileDTO responseDTO = userService.findProfile(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/accounts/profile")
    public ResponseEntity<?> updateProfile(@RequestBody @Valid UserRequest.UpdateProfileDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.updateProfile(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PatchMapping("/auth/access")
    public ResponseEntity<?> updateAccessToken(@CookieValue String refreshToken) {
        TokenResponse tokenResponse = userService.updateAccessToken(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtUtils.refreshTokenCookie(tokenResponse.refreshToken()))
                .body(ApiUtils.success(HttpStatus.OK, new UserResponse.AccessTokenDTO(tokenResponse.accessToken())));
    }

    @DeleteMapping("/accounts/withdraw")
    public ResponseEntity<?> withdrawMember(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.withdrawMember(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/validate/accessToken")
    public ResponseEntity<?> validateAccessToken(@CookieValue String accessToken) {
        UserResponse.ValidateAccessTokenDTO responseDTO = userService.validateAccessToken(accessToken);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/communityStats")
    public ResponseEntity<?> findCommunityStats(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse.FindCommunityRecord responseDTO = userService.findCommunityStats(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}