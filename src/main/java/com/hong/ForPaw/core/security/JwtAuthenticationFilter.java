package com.hong.ForPaw.core.security;

import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hong.ForPaw.domain.User.UserRole;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.service.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Slf4j
public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

    private RedisService redisService;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, RedisService redisService) {
        super(authenticationManager);
        this.redisService = redisService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String authorizationHeader = request.getHeader(JWTProvider.AUTHORIZATION);

        String refreshToken = getCookieFromRequest(JWTProvider.REFRESH_TOKEN_COOKIE_KEY, request);
        String accessToken = (authorizationHeader != null && authorizationHeader.startsWith(JWTProvider.TOKEN_PREFIX)) ?
                authorizationHeader.replace(JWTProvider.TOKEN_PREFIX, "")  : null;

        if (authorizationHeader == null && (refreshToken == null || refreshToken.trim().isEmpty())) {
            chain.doFilter(request, response);
            return;
        }

        User user = null;

        // accessToken 검증
        if(accessToken != null) {
            user = getUserFromToken(accessToken);
        }

        // refreshToken 검증
        if (user == null && refreshToken != null) {
            user = getUserFromToken(refreshToken);

            if (user != null) {
                accessToken = JWTProvider.createAccessToken(user);
                refreshToken = JWTProvider.createRefreshToken(user);

                setCookieToResponse(JWTProvider.ACCESS_TOKEN_COOKIE_KEY, accessToken, JWTProvider.ACCESS_EXP_SEC, false, true, response);
                setCookieToResponse(JWTProvider.REFRESH_TOKEN_COOKIE_KEY, refreshToken, JWTProvider.REFRESH_EXP_SEC, false, true, response);
            }
        }

        // accessToken과 refreshToken을 모두 거쳤음에도 null
        if (user == null) {
            chain.doFilter(request, response);
            return;
        }

        // 로그인 진행
        authenticateUser(user);

        // Request 쿠키와 Response 쿠키 동기화
        syncHttpResponseCookiesFromHttpRequest(request, response,
                JWTProvider.ACCESS_TOKEN_COOKIE_KEY,
                JWTProvider.REFRESH_TOKEN_COOKIE_KEY);

        // ACCESS 토큰은 HTTP Header로 리턴
        response.setHeader(HttpHeaders.AUTHORIZATION, JWTProvider.TOKEN_PREFIX + accessToken);

        chain.doFilter(request, response);
    }

    private void authenticateUser(User user) {
        CustomUserDetails myUserDetails = new CustomUserDetails(user);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        myUserDetails,
                        myUserDetails.getPassword(),
                        myUserDetails.getAuthorities()
                );

        redisService.addSetElement(createVisitKey(), user.getId());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User getUserFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWTProvider.verify(token);
            Long id = decodedJWT.getClaim("id").asLong();
            UserRole userRole = decodedJWT.getClaim("role").as(UserRole.class);
            String nickName = decodedJWT.getClaim("nickName").asString();
            return User.builder().id(id).role(userRole).nickName(nickName).build();
        } catch (SignatureVerificationException sve) {
            log.error("토큰 검증 실패");
        } catch (TokenExpiredException tee) {
            log.error("토큰 만료됨");
        }
        return null;
    }

    private String createVisitKey() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

        return "visit" + ":" + now.format(formatter);
    }

    private String getCookieFromRequest(String cookieKey, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieKey.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setCookieToResponse(String cookieKey, String cookieValue, Long maxAgeSec, boolean secureCookie, boolean isHttpOnly, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieKey, cookieValue)
                .httpOnly(isHttpOnly)
                .secure(secureCookie)
                .maxAge(maxAgeSec)
                .path("/")
                .sameSite("None")
                .build();

        // 쿠키를 응답 헤더에 추가 (기존에 쿠키가 있더라도 새로 추가)
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private HttpServletResponse syncHttpResponseCookiesFromHttpRequest(HttpServletRequest request, HttpServletResponse response, String... exceptionKeys) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String cookieKey = cookie.getName();

                // exceptionKey 배열에 포함된 쿠키 키는 제외
                if (Arrays.asList(exceptionKeys).contains(cookieKey)) {
                    continue;
                }

                ResponseCookie responseCookie = ResponseCookie.from(cookieKey, cookie.getValue())
                        .httpOnly(cookie.isHttpOnly())
                        .secure(cookie.getSecure())
                        .maxAge(cookie.getMaxAge())
                        .path("/")
                        .sameSite("None")
                        .build();

                response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
            }
        }
        return response;
    }
}