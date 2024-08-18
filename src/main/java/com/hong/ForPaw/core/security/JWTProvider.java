package com.hong.ForPaw.core.security;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hong.ForPaw.domain.User.User;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import java.util.Date;


@Component
public class JWTProvider {

    public static final Long ACCESS_EXP_MILLI = 1000L * 60 * 60 * 24; // 1시간
    public static final Long REFRESH_EXP_MILLI = 1000L * 60 * 60 * 24 * 7; // 일주일

    public static final Long ACCESS_EXP_SEC = 60L * 60 * 24; // 1시간
    public static final Long REFRESH_EXP_SEC = 60L * 60 * 24 * 7; // 일주일

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String AUTHORIZATION = "Authorization";
    public static final String SECRET = "MySecretKey";
    public static final String REFRESH_TOKEN_COOKIE_KEY = "refreshToken";
    public static final String ACCESS_TOKEN_COOKIE_KEY = "accessToken";


    // access token 생성
    public static String createAccessToken(User user) {
        return create(user, ACCESS_EXP_MILLI);
    }

    // refresh token 생성
    public static String createRefreshToken(User user) {
        return create(user, REFRESH_EXP_MILLI);
    }

    public static String create(User user, Long exp) {
        return JWT.create()
                .withSubject(user.getEmail())
                .withExpiresAt(new Date(System.currentTimeMillis() + exp))
                .withClaim("id", user.getId())
                .withClaim("role", user.getRole().ordinal())
                .withClaim("nickName", user.getNickName())
                .sign(Algorithm.HMAC512(SECRET));
    }

    public static DecodedJWT verify(String jwt) throws SignatureVerificationException, TokenExpiredException {
        // "Bearer " 접두사가 있다면 제거
        jwt = jwt.replace(JWTProvider.TOKEN_PREFIX, "");
        return JWT.require(Algorithm.HMAC512(SECRET))
                .build().verify(jwt);
    }

    public static Long getUserIdFromToken(String token) {
        DecodedJWT decodedJWT = verify(token);
        return decodedJWT.getClaim("id").asLong();
    }

    public static boolean validateToken(String token) {
        try {
            token = token.replace(JWTProvider.TOKEN_PREFIX, "");
            JWT.require(Algorithm.HMAC512(SECRET)).build().verify(token);
            return true;
        } catch (JWTVerificationException exception) { // 잘못된 서명 등 디코딩이 안되는 잘못된 토큰
            return false;
        }
    }
}
