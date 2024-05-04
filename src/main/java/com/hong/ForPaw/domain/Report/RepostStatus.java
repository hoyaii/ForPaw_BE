package com.hong.ForPaw.domain.Report;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RepostStatus {

    PROCESSING("진행중"),
    PROCESSED("완료됨");

    private String value;
}
