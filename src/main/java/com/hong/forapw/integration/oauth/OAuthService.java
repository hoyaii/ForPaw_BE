package com.hong.forapw.integration.oauth;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.common.utils.JwtUtils;
import com.hong.forapw.domain.user.model.LoginResult;
import com.hong.forapw.domain.user.service.UserService;
import com.hong.forapw.integration.oauth.google.GoogleOAuthService;
import com.hong.forapw.integration.oauth.google.GoogleOAuthToken;
import com.hong.forapw.integration.oauth.google.GoogleUser;
import com.hong.forapw.integration.oauth.kakao.KakaoOAuthService;
import com.hong.forapw.integration.oauth.kakao.KakaoOAuthToken;
import com.hong.forapw.integration.oauth.kakao.KakaoUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.hong.forapw.common.GlobalConstants.ACCESS_TOKEN_KEY;
import static com.hong.forapw.common.GlobalConstants.REFRESH_TOKEN_KEY;
import static com.hong.forapw.domain.user.model.LoginResult.isJoined;
import static com.hong.forapw.domain.user.model.LoginResult.isNotJoined;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final GoogleOAuthService googleOAuthService;
    private final KakaoOAuthService kakaoOAuthService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @Value("${social.join.redirect.uri}")
    private String redirectJoinUri;

    @Value("${social.home.redirect.uri}")
    private String redirectHomeUri;

    @Value("${social.login.redirect.uri}")
    private String redirectLoginUri;

    private static final String QUERY_PARAM_EMAIL = "email";
    private static final String QUERY_PARAM_AUTH_PROVIDER = "authProvider";
    private static final String QUERY_PARAM_ACCESS_TOKEN = "accessToken";

    @Transactional
    public LoginResult loginWithKakao(String code, HttpServletRequest request) {
        KakaoOAuthToken token = kakaoOAuthService.getToken(code);
        KakaoUser kakaoUser = kakaoOAuthService.getUserInfo(token.access_token());
        String email = kakaoUser.kakao_account().email();

        return userService.processSocialLogin(email, request);
    }

    @Transactional
    public LoginResult loginWithGoogle(String code, HttpServletRequest request) {
        GoogleOAuthToken token = googleOAuthService.getToken(code);
        GoogleUser googleUser = googleOAuthService.getUserInfo(token.access_token());
        String email = googleUser.email();

        return userService.processSocialLogin(email, request);
    }

    public void redirectAfterOAuthLogin(LoginResult loginResult, String authProvider, HttpServletResponse response) {
        try {
            String redirectUri = buildRedirectUri(loginResult, authProvider, response);
            response.sendRedirect(redirectUri);
        } catch (IOException e) {
            log.error("소셜 로그인 증 리다이렉트 에러 발생", e);
            throw new CustomException(ExceptionCode.REDIRECT_FAILED);
        }
    }

    private String buildRedirectUri(LoginResult loginResult, String authProvider, HttpServletResponse response) {
        if (isNotJoined(loginResult)) {
            return createRedirectUri(redirectJoinUri, Map.of(
                    QUERY_PARAM_EMAIL, loginResult.email(),
                    QUERY_PARAM_AUTH_PROVIDER, authProvider
            ));
        } else if (isJoined(loginResult)) {
            String refreshToken = loginResult.refreshToken();
            String accessToken = loginResult.accessToken();
            response.addHeader(HttpHeaders.SET_COOKIE, jwtUtils.refreshTokenCookie(refreshToken));

            return createRedirectUri(redirectHomeUri, Map.of(
                    QUERY_PARAM_ACCESS_TOKEN, accessToken,
                    QUERY_PARAM_AUTH_PROVIDER, authProvider
            ));
        } else {
            return createRedirectUri(redirectLoginUri, Map.of(QUERY_PARAM_AUTH_PROVIDER, authProvider));
        }
    }

    private String createRedirectUri(String baseUri, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUri);
        queryParams.forEach((key, value) ->
                builder.queryParam(key, URLEncoder.encode(value, StandardCharsets.UTF_8))
        );
        return builder.build().toUriString();
    }
}