package com.hong.ForPaw.domain.Report;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ReportTargetType {

    POST("게시글"),
    COMMENT("댓글");

    private String description;
}
