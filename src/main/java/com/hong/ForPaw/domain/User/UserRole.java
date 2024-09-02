package com.hong.ForPaw.domain.User;

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
