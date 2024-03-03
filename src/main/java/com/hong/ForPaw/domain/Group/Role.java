package com.hong.ForPaw.domain.Group;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Role {
    TEMP("임시"),
    REJECTED("거절됨"),
    USER("유저"),
    ADMIN("관리자");

    private String value;
}
