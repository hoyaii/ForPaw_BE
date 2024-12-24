package com.hong.forapw.domain.chat.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class LinkMetadata {

    private String title;
    private String description;
    private String image;
    private String ogUrl;
    private String icon;

    @Builder
    public LinkMetadata(String title, String description, String image, String ogUrl, String icon) {
        this.title = title;
        this.description = description;
        this.image = image;
        this.ogUrl = ogUrl;
        this.icon = icon;
    }
}
