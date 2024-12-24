package com.hong.forapw.domain.user.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AuthProvider {

    LOCAL("로컬"),
    GOOGLE("구글"),
    KAKAO("카카오");

    private String value;
}
