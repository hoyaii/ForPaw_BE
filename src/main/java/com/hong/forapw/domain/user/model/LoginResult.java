package com.hong.forapw.domain.user.model;

public record LoginResult(
        String email,
        String accessToken,
        String refreshToken,
        boolean isJoined
) {
    public static boolean isJoined(LoginResult loginResult) {
        return loginResult.isJoined();
    }

    public static boolean isNotJoined(LoginResult loginResult) {
        return !loginResult.isJoined();
    }
}
