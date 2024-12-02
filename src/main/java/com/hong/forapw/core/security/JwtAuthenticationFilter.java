package com.hong.forapw.core.security;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hong.forapw.core.utils.CookieUtils;
import com.hong.forapw.domain.user.UserRole;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.service.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static com.hong.forapw.core.utils.DateTimeUtils.DATE_HOUR_FORMAT;
import static com.hong.forapw.core.utils.DateTimeUtils.formatLocalDateTime;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RedisService redisService;

    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String ACCESS_TOKEN = "accessToken";

    public JwtAuthenticationFilter(RedisService redisService) {
        this.redisService = redisService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws IOException, ServletException {
        String accessToken = extractAccessToken(request);
        String refreshToken = extractRefreshToken(request);

        if (areTokensEmpty(accessToken, refreshToken)) {
            chain.doFilter(request, response);
            return;
        }

        User user = authenticateWithTokens(accessToken, refreshToken);
        if (user == null) {
            chain.doFilter(request, response);
            return;
        }

        authenticateUser(user);
        recordUserVisit(user);
        syncRequestResponseCookies(request, response);

        chain.doFilter(request, response);
    }

    private String extractAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(JWTProvider.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith(JWTProvider.TOKEN_PREFIX)) {
            return authorizationHeader.replace(JWTProvider.TOKEN_PREFIX, "");
        }
        return null;
    }

    private String extractRefreshToken(HttpServletRequest request) {
        return CookieUtils.getFromRequest(JWTProvider.REFRESH_TOKEN_KEY_PREFIX, request);
    }

    private boolean areTokensEmpty(String accessToken, String refreshToken) {
        return (accessToken == null || accessToken.trim().isEmpty()) &&
                (refreshToken == null || refreshToken.trim().isEmpty());
    }

    private User authenticateWithTokens(String accessToken, String refreshToken) {
        return Optional.ofNullable(authenticateAccessToken(accessToken))
                .orElseGet(() -> authenticateRefreshToken(refreshToken));
    }

    private User authenticateAccessToken(String accessToken) {
        return Optional.ofNullable(accessToken)
                .map(this::getUserFromToken)
                .orElse(null);
    }

    private User authenticateRefreshToken(String refreshToken) {
        return Optional.ofNullable(refreshToken)
                .flatMap(token -> Optional.ofNullable(getUserFromToken(token))
                        .filter(user -> isRefreshTokenValid(user, refreshToken)))
                .orElse(null);
    }

    private boolean isRefreshTokenValid(User user, String refreshToken) {
        return redisService.isStoredValue(REFRESH_TOKEN, String.valueOf(user.getId()), refreshToken);
    }

    private void authenticateUser(User user) {
        CustomUserDetails myUserDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                myUserDetails,
                null,
                myUserDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void recordUserVisit(User user) {
        String visitKey = "visit" + ":" + formatLocalDateTime(LocalDateTime.now(), DATE_HOUR_FORMAT);
        redisService.addSetElement(visitKey, user.getId());
    }

    private void syncRequestResponseCookies(HttpServletRequest request, HttpServletResponse response) {
        Set<String> excludedCookies = Set.of(ACCESS_TOKEN, REFRESH_TOKEN);
        CookieUtils.syncRequestCookiesToResponse(request, response, excludedCookies);
    }

    private User getUserFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWTProvider.decodeJWT(token);

            Long id = decodedJWT.getClaim("id").asLong();
            UserRole role = decodedJWT.getClaim("role").as(UserRole.class);
            String nickname = decodedJWT.getClaim("nickName").asString();
            return User.builder().id(id).role(role).nickName(nickname).build();
        } catch (SignatureVerificationException sve) {
            log.error("토큰 유효성 검증 실패 {}: {}", token, sve.getMessage());
        } catch (TokenExpiredException tee) {
            log.error("토큰이 만료 되었음 {}: {}", token, tee.getMessage());
        } catch (JWTDecodeException jde) {
            log.error("토큰 디코딩 실패 {}: {}", token, jde.getMessage());
        }
        return null;
    }
}