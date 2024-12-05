package com.hong.forapw.core.utils;

import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UriUtils {

    private UriUtils() {
    }

    public static String buildRedirectUri(String baseUri, Map<String, String> queryParams) {
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
}