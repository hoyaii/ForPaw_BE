package com.hong.forapw.domain.user.service;

import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.model.LoginResult;
import com.hong.forapw.integration.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final RedisService redisService;

    @Value("${jwt.access-exp-milli}")
    public Long accessExpMilli;

    @Value("${jwt.refresh-exp-milli}")
    public Long refreshExpMilli;

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refreshToken";
    private static final String ACCESS_TOKEN_KEY_PREFIX = "accessToken";
    private static final long VERIFICATION_CODE_EXPIRATION_MS = 175 * 1000L;
    private static final String EMAIL_CODE_KEY_PREFIX = "code:";
    private static final String CODE_TO_EMAIL_KEY_PREFIX = "codeToEmail";
    private static final long LOGIN_FAIL_CURRENT_EXPIRATION_MS = 300_000L; // 5분
    private static final String MAX_LOGIN_ATTEMPTS_BEFORE_LOCK = "loginFail";
    private static final String MAX_DAILY_LOGIN_FAILURES = "loginFailDaily";
    private static final long LOGIN_FAIL_DAILY_EXPIRATION_MS = 86400000L; // 24시간

    public void storeAccessToken(Long userId, String accessToken) {
        redisService.storeValue(ACCESS_TOKEN_KEY_PREFIX, userId.toString(), accessToken, accessExpMilli);
    }

    public void storeVerificationCode(String email, String codeType, String verificationCode) {
        redisService.storeValue(getCodeTypeKey(codeType), email, verificationCode, VERIFICATION_CODE_EXPIRATION_MS);
    }

    public void storeCodeToEmail(String verificationCode, String email) {
        redisService.storeValue(CODE_TO_EMAIL_KEY_PREFIX, verificationCode, email, LOGIN_FAIL_CURRENT_EXPIRATION_MS);
    }

    public void storeUserTokens(Long userId, LoginResult loginResult) {
        redisService.storeValue(ACCESS_TOKEN_KEY_PREFIX, userId.toString(), loginResult.accessToken(), accessExpMilli);
        redisService.storeValue(REFRESH_TOKEN_KEY_PREFIX, userId.toString(), loginResult.refreshToken(), refreshExpMilli);
    }

    public long incrementDailyLoginFailures(User user) {
        long dailyFailures = redisService.getValueInLong(MAX_DAILY_LOGIN_FAILURES, user.getId().toString());
        dailyFailures++;
        redisService.storeValue(MAX_DAILY_LOGIN_FAILURES, user.getId().toString(), Long.toString(dailyFailures), LOGIN_FAIL_DAILY_EXPIRATION_MS);
        return dailyFailures;
    }

    public long incrementCurrentLoginFailures(Long userId) {
        long currentFailures = redisService.getValueInLong(MAX_LOGIN_ATTEMPTS_BEFORE_LOCK, userId.toString());
        currentFailures++;
        redisService.storeValue(MAX_LOGIN_ATTEMPTS_BEFORE_LOCK, userId.toString(), Long.toString(currentFailures), LOGIN_FAIL_CURRENT_EXPIRATION_MS);
        return currentFailures;
    }

    public void deleteUserTokens(Long userId) {
        redisService.removeValue(ACCESS_TOKEN_KEY_PREFIX, userId.toString());
        redisService.removeValue(REFRESH_TOKEN_KEY_PREFIX, userId.toString());
    }

    public void deleteCodeToEmail(String verificationCode) {
        redisService.removeValue(CODE_TO_EMAIL_KEY_PREFIX, verificationCode);
    }

    public void validateAccessToken(String accessToken, Long userId) {
        if (!redisService.doesValueMatch(ACCESS_TOKEN_KEY_PREFIX, userId.toString(), accessToken)) {
            throw new CustomException(ExceptionCode.ACCESS_TOKEN_WRONG);
        }
    }

    public void validateEmailCodeNotSent(String email, String codeType) {
        if (redisService.isValueStored(getCodeTypeKey(codeType), email)) {
            throw new CustomException(ExceptionCode.ALREADY_SEND_EMAIL);
        }
    }

    public boolean isCodeMismatch(String email, String code, String codeType) {
        return redisService.doesValueMismatch(getCodeTypeKey(codeType), email, code);
    }

    public long getDailyLoginFailures(Long userId) {
        return redisService.getValueInLong(MAX_DAILY_LOGIN_FAILURES, userId.toString());
    }

    public long getCurrentLoginFailures(Long userId) {
        return redisService.getValueInLong(MAX_LOGIN_ATTEMPTS_BEFORE_LOCK, userId.toString());
    }

    public String getValidRefreshToken(Long userId) {
        String refreshToken = redisService.getValueInString(REFRESH_TOKEN_KEY_PREFIX, userId.toString());
        if (refreshToken == null)
            throw new CustomException(ExceptionCode.TOKEN_EXPIRED);

        return refreshToken;
    }

    public String getEmailByVerificationCode(String verificationCode) {
        return redisService.getValueInString(CODE_TO_EMAIL_KEY_PREFIX, verificationCode);
    }

    private String getCodeTypeKey(String codeType) {
        return EMAIL_CODE_KEY_PREFIX + codeType;
    }
}
