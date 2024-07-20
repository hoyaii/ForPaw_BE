package com.hong.ForPaw.domain.Inquiry;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum InquiryType {

    ADOPTION("입양 문의"),
    USE("이용 문의"),
    ERROR("오류 문의"),
    OTHER("기타 문의");

    private String value;
}
