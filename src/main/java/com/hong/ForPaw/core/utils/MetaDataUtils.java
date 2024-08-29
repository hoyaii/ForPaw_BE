package com.hong.ForPaw.core.utils;

import com.hong.ForPaw.domain.Chat.LinkMetadata;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class MetaDataUtils {

    public static LinkMetadata fetchMetadata(String url) {
        // Jsoup을 사용하여 파싱
        Document doc = null;
        try {
            doc = Jsoup.connect(url)
                    .userAgent("PostmanRuntime/7.41.2")
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 메타데이터 추출
        String title = getMetaTagContent(doc, "og:title", "title");
        String description = getMetaTagContent(doc, "og:description", "meta[name=description]");
        String image = getMetaTagContent(doc, "og:image", null);
        String ogUrl = getMetaTagContent(doc, "og:url", null);

        // 아이콘 추출
        String icon = getIconLink(doc);

        return new LinkMetadata(title, description, image, ogUrl != null ? ogUrl : url, icon);
    }

    private static String getMetaTagContent(Document doc, String ogTag, String fallbackTag) {
        Element metaTag = doc.selectFirst("meta[property=" + ogTag + "]");
        if (metaTag != null) {
            return metaTag.attr("content"); // 메타 태그의 'content' 속성 값을 반환
        }

        // 만약 Open Graph 태그가 없고, fallbackTag가 지정되어 있다면 해당 태그를 검색
        if (fallbackTag != null) {
            Element fallbackMetaTag = doc.selectFirst(fallbackTag);
            if (fallbackMetaTag != null) {
                return fallbackMetaTag.text();
            }
        }
        return null;
    }

    private static String getIconLink(Document doc) {
        Element iconTag = doc.selectFirst("link[rel=icon]");

        // 만약 기본 아이콘이 없다면 apple-touch-icon 검색
        if (iconTag == null) {
            iconTag = doc.selectFirst("link[rel=apple-touch-icon]");
        }

        // 아이콘 링크 반환
        if (iconTag != null) {
            return iconTag.attr("href");
        }
        return null;
    }
}
