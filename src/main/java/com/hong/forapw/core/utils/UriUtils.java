package com.hong.forapw.core.utils;

import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UriUtils {

    private UriUtils() {
    }

    @Value("${kakao.map.geocoding.uri}")
    private static String kakaoGeoCodingURI;

    @Value("${google.map.geocoding.uri}")
    private static String googleGeoCodingURI;

    @Value("${google.api.key}")
    private static String googleAPIKey;

    @Value("${openAPI.animal.uri}")
    private static String animalURI;

    public static String createRedirectUri(String baseUri, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUri);
        queryParams.forEach((key, value) ->
                builder.queryParam(key, URLEncoder.encode(value, StandardCharsets.UTF_8))
        );
        return builder.build().toUriString();
    }

    public static String convertHttpUrlToHttps(String url) {
        if (StringUtils.isBlank(url)) {
            throw new CustomException(ExceptionCode.INVALID_URI_FORMAT);
        }

        return StringUtils.replaceOnce(url, "http://", "https://");
    }

    public static Mono<URI> createAnimalOpenApiURI(String serviceKey, Long careRegNo) {
        String url = animalURI + "?serviceKey=" + serviceKey + "&care_reg_no=" + careRegNo + "&_type=json" + "&numOfRows=1000";
        try {
            return Mono.just(new URI(url));
        } catch (URISyntaxException e) {
            return Mono.empty();
        }
    }

    public static URI createShelterOpenApiURI(String baseUrl, String serviceKey, Integer uprCd, Integer orgCd) {
        String uri = baseUrl + "?serviceKey=" + serviceKey + "&upr_cd=" + uprCd + "&org_cd=" + orgCd + "&_type=json";
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new CustomException(ExceptionCode.INVALID_URI_FORMAT);
        }
    }

    public static URI createKakaoGeocodingURI(String address) {
        if (address == null || address.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_URI_FORMAT);
        }

        return UriComponentsBuilder.fromHttpUrl(kakaoGeoCodingURI)
                .queryParam("query", address)
                .build()
                .encode()
                .toUri();
    }

    public static URI createGoogleGeocodingURI(String address) {
        if (address == null || address.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_URI_FORMAT);
        }

        return UriComponentsBuilder.fromHttpUrl(googleGeoCodingURI)
                .queryParam("address", address)
                .queryParam("key", googleAPIKey)
                .build()
                .encode()
                .toUri();
    }
}