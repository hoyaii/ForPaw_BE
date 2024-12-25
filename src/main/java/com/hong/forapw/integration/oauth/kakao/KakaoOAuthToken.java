package com.hong.forapw.integration.oauth.kakao;

import com.hong.forapw.integration.oauth.common.OAuthToken;

public record KakaoOAuthToken(
        String token_type,
        String access_token,
        Integer expires_in,
        String refresh_token,
        Integer refresh_token_expires_in,
        String scope) implements OAuthToken {
}