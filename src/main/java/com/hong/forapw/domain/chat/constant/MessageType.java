package com.hong.forapw.domain.chat.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MessageType {

    TEXT("텍스트"),
    IMAGE("이미지"),
    FILE("기타파일"),
    LINK("링크");

    private String value;
}
