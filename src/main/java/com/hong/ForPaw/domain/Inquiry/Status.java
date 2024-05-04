package com.hong.ForPaw.domain.Inquiry;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Status {

    PROCESSING("진행중"),
    PROCESSED("처리됨");

    private String value;
}
