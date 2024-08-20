package com.hong.ForPaw.core.utils;

import com.hong.ForPaw.domain.Chat.LinkMetadata;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.jsoup.nodes.Document;

import java.util.Objects;

public class MetaDataUtils {

    public static LinkMetadata fetchMetadata(String url) {
        // User-Agent 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String html = response.getBody();

        // JSoup을 사용하여 HTML 파싱
        Document doc = Jsoup.parse(Objects.requireNonNull(html));

        // 메타데이터 추출
        String title = getMetaTagContent(doc, "og:title", "title");
        String description = getMetaTagContent(doc, "og:description", "meta[name=description]");
        String image = getMetaTagContent(doc, "og:image", null);
        String ogUrl = getMetaTagContent(doc, "og:url", null);

        return new LinkMetadata(title, description, image, ogUrl != null ? ogUrl : url);
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
}
