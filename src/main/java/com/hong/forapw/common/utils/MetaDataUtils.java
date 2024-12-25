package com.hong.forapw.common.utils;

import com.hong.forapw.domain.chat.entity.LinkMetadata;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Optional;

public class MetaDataUtils {

    private MetaDataUtils() {
    }

    public static LinkMetadata fetchMetadata(String url) {
        try {
            Document doc = fetchDocument(url);

            String title = extractMetaContent(doc, "og:title", "title");
            String description = extractMetaContent(doc, "og:description", "meta[name=description]");
            String image = extractMetaContent(doc, "og:image", null);
            String ogUrl = extractMetaContent(doc, "og:url", null);
            String icon = extractIconLink(doc);

            return new LinkMetadata(title, description, image, ogUrl != null ? ogUrl : url, icon);
        } catch (IOException e) {
            return null;
        }
    }

    private static Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("PostmanRuntime/7.41.2")
                .header("Content-Type", "application/json")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .get();
    }

    private static String extractMetaContent(Document doc, String ogTag, String fallbackTag) {
        return Optional.ofNullable(doc.selectFirst("meta[property=" + ogTag + "]"))
                .map(meta -> meta.attr("content"))
                .orElseGet(() -> fallbackTag != null ? extractFallbackContent(doc, fallbackTag) : null);
    }

    private static String extractFallbackContent(Document doc, String fallbackTag) {
        return Optional.ofNullable(doc.selectFirst(fallbackTag))
                .map(Element::text)
                .orElse(null);
    }

    private static String extractIconLink(Document doc) {
        return Optional.ofNullable(doc.selectFirst("link[rel=icon]"))
                .or(() -> Optional.ofNullable(doc.selectFirst("link[rel=apple-touch-icon]"))) // 만약 기본 아이콘이 없다면 apple-touch-icon 검색
                .map(icon -> icon.attr("href"))
                .orElse(null);
    }
}
