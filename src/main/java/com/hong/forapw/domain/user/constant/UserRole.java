package com.hong.forapw.domain.user.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserRole {

    USER("유저"),
    ADMIN("관리자"),
    SUPER("슈퍼"),
    SHELTER("보호소");

    private String value;
}
