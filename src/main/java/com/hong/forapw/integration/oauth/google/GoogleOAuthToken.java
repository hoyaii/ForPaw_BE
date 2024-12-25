package com.hong.forapw.integration.oauth.google;

import com.hong.forapw.integration.oauth.common.OAuthToken;

public record GoogleOAuthToken(
        String access_token,
        Long expires_in,
        String token_type,
        String scope,
        String refresh_token) implements OAuthToken {
}