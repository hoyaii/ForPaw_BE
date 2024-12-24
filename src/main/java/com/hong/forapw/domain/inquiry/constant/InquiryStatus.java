package com.hong.forapw.domain.inquiry.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum InquiryStatus {

    PROCESSING("진행중"),
    PROCESSED("처리됨");

    private String value;
}
