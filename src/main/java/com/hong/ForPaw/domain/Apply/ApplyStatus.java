package com.hong.ForPaw.domain.Apply;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ApplyStatus {

    PROCESSING("진행중"),
    REJECTED("반려됨"),
    PROCESSED("완료됨");

    private String value;
}
