package com.hong.forapw.domain.faq;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FaqType {

    INQUIRY("이용 문의"),
    ADOPTION("입양"),
    VOLUNTEERING("봉사활동"),
    COMMUNITY("커뮤니티"),
    MEMBERSHIP("회원/계정"),
    OTHERS("기타");

    private final String value;
}
