package com.hong.forapw.integration.oauth.kakao;

import com.hong.forapw.integration.oauth.common.SocialOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import static com.hong.forapw.common.GlobalConstants.*;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService implements SocialOAuthService<KakaoOAuthToken, KakaoUser> {

    private final WebClient webClient;

    @Value("${kakao.key}")
    private String kakaoApiKey;

    @Value("${kakao.oauth.token.uri}")
    private String kakaoTokenUri;

    @Value("${kakao.oauth.userInfo.uri}")
    private String kakaoUserInfoUri;


    @Override
    public KakaoOAuthToken getToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", AUTH_CODE_GRANT_TYPE);
        formData.add("client_id", kakaoApiKey);
        formData.add("code", code);

        return webClient.post()
                .uri(kakaoTokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(KakaoOAuthToken.class)
                .block();
    }

    @Override
    public KakaoUser getUserInfo(String accessToken) {
        return webClient.get()
                .uri(kakaoUserInfoUri)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken)
                .retrieve()
                .bodyToMono(KakaoUser.class)
                .block();
    }
}
