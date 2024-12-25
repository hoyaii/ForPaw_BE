package com.hong.forapw.admin.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ContentType {

    POST("게시글"),
    COMMENT("댓글");

    private final String description;

    public boolean isNotValidTypeForReport() {
        return !(this == ContentType.POST || this == ContentType.COMMENT);
    }
}
