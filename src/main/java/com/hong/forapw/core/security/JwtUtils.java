package com.hong.forapw.core.security;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.domain.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;

import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.access-exp-milli}")
    public Long accessExpMilli; // 1시간

    @Value("${jwt.refresh-exp-milli}")
    public Long refreshExpMilli; // 일주일

    @Value("${jwt.refresh-exp-milli}")
    public Long refreshExpSec;

    @Value("${jwt.secret}")
    public String secret;

    public static final String AUTHORIZATION = "Authorization";
    public static final String REFRESH_TOKEN_KEY_PREFIX = "refreshToken";

    public String refreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_KEY_PREFIX, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(refreshExpSec)
                .build().toString();
    }

    public String createAccessToken(User user) {
        return create(user, accessExpMilli);
    }

    public String createRefreshToken(User user) {
        return create(user, refreshExpMilli);
    }

    public String create(User user, Long exp) {
        return JWT.create()
                .withSubject(user.getEmail())
                .withExpiresAt(new Date(System.currentTimeMillis() + exp))
                .withClaim("id", user.getId())
                .withClaim("role", user.getRole().ordinal())
                .withClaim("nickName", user.getNickname())
                .sign(Algorithm.HMAC512(secret));
    }

    public DecodedJWT decodeJWT(String encodedJWT) throws SignatureVerificationException, TokenExpiredException {
        return JWT.require(Algorithm.HMAC512(secret))
                .build()
                .verify(encodedJWT);
    }

    public Long extractUserId(String token) {
        DecodedJWT decodedJWT = decodeJWT(token);
        return decodedJWT.getClaim("id").asLong();
    }

    public void validateTokenFormat(String refreshToken) {
        try {
            decodeJWT(refreshToken);
        } catch (TokenExpiredException e) {
            throw new CustomException(ExceptionCode.TOKEN_EXPIRED);
        } catch (SignatureVerificationException e) {
            throw new CustomException(ExceptionCode.TOKEN_INVALID_SIGNATURE);
        } catch (JWTVerificationException e) {
            throw new CustomException(ExceptionCode.TOKEN_WRONG);
        }
    }
}
