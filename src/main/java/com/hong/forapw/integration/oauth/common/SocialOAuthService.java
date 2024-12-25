package com.hong.forapw.integration.oauth.common;

public interface SocialOAuthService<T extends OAuthToken, U extends SocialUser> {

    T getToken(String code);

    U getUserInfo(String accessToken);
}