package com.hong.ForPaw.domain.Group;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Role {
    USER("유저"),
    ADMIN("관리자");

    private String value;
}
