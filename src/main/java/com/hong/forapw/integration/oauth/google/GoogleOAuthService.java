package com.hong.forapw.integration.oauth.google;

import com.hong.forapw.integration.oauth.common.SocialOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static com.hong.forapw.common.GlobalConstants.*;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService implements SocialOAuthService<GoogleOAuthToken, GoogleUser> {

    private final WebClient webClient;

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${google.oauth.token.uri}")
    private String googleTokenUri;

    @Value("${google.client.password}")
    private String googleClientSecret;

    @Value("${google.oauth.redirect.uri}")
    private String googleRedirectUri;

    @Value("${google.oauth.userInfo.uri}")
    private String googleUserInfoUri;


    @Override
    public GoogleOAuthToken getToken(String code) {
        String decode = URLDecoder.decode(code, StandardCharsets.UTF_8);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", decode);
        formData.add("client_id", googleClientId);
        formData.add("client_secret", googleClientSecret);
        formData.add("redirect_uri", googleRedirectUri);
        formData.add("grant_type", AUTH_CODE_GRANT_TYPE);

        return webClient.post()
                .uri(googleTokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(GoogleOAuthToken.class)
                .block();
    }

    @Override
    public GoogleUser getUserInfo(String accessToken) {
        return webClient.get()
                .uri(googleUserInfoUri)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
                .retrieve()
                .bodyToMono(GoogleUser.class)
                .block();
    }
}