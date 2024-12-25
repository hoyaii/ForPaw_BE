package com.hong.forapw.domain.user.model;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
