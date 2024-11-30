package com.hong.forapw.core.security;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hong.forapw.domain.user.User;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;

import java.util.Date;


@Component
public class JWTProvider {

    private JWTProvider() {
    }

    public static final Long ACCESS_EXP_MILLI = 1000L * 60 * 60 * 24; // 1시간
    public static final Long REFRESH_EXP_MILLI = 1000L * 60 * 60 * 24 * 7; // 일주일
    public static final Long REFRESH_EXP_SEC = 60L * 60 * 24 * 7; // 일주일
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String AUTHORIZATION = "Authorization";
    public static final String SECRET = "MySecretKey";
    public static final String REFRESH_TOKEN_KEY_PREFIX = "refreshToken";

    public static String createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_KEY_PREFIX, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(JWTProvider.REFRESH_EXP_SEC)
                .build().toString();
    }

    public static String createAccessTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_KEY_PREFIX, refreshToken)
                .httpOnly(false)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(JWTProvider.REFRESH_EXP_SEC)
                .build().toString();
    }

    public static String createAccessToken(User user) {
        return create(user, ACCESS_EXP_MILLI);
    }

    public static String createRefreshToken(User user) {
        return create(user, REFRESH_EXP_MILLI);
    }

    public static String create(User user, Long exp) {
        return JWT.create()
                .withSubject(user.getEmail())
                .withExpiresAt(new Date(System.currentTimeMillis() + exp))
                .withClaim("id", user.getId())
                .withClaim("role", user.getRole().ordinal())
                .withClaim("nickName", user.getNickname())
                .sign(Algorithm.HMAC512(SECRET));
    }

    public static DecodedJWT decodeJWT(String jwt) throws SignatureVerificationException, TokenExpiredException {
        jwt = removeTokenPrefix(jwt);
        return JWT.require(Algorithm.HMAC512(SECRET))
                .build()
                .verify(jwt);
    }

    public static Long extractUserIdFromToken(String token) {
        DecodedJWT decodedJWT = decodeJWT(token);
        return decodedJWT.getClaim("id").asLong();
    }

    public static boolean isInvalidJwtFormat(String jwt) {
        try {
            decodeJWT(jwt);
            return false;
        } catch (JWTVerificationException exception) { // 잘못된 서명 등 디코딩이 안되는 잘못된 토큰
            return true;
        }
    }

    private static String removeTokenPrefix(String jwt) {
        return jwt.replace(JWTProvider.TOKEN_PREFIX, "");
    }
}
