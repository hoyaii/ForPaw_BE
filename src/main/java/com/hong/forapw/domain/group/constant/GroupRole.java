package com.hong.forapw.domain.group.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GroupRole {

    TEMP("임시"),
    USER("유저"),
    ADMIN("관리자"),
    CREATOR("그룹장");

    private String value;
}
