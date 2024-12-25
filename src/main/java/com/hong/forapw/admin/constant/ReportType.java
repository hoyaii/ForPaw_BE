package com.hong.forapw.admin.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ReportType {

    INAPPROPRIATE_FOR_FORUM_NATURE("게시판 성격에 부적절함"),
    COMMERCIAL_ADVERTISING("상업적 광고"),
    PROFANITY("욕설"),
    CLICKBAIT_FLOODING("낚시/도배"),
    LEAKS_IMPERSONATION_FRAUD("유출/사칭/사기"),
    POLITICAL_DISPARAGEMENT_AND_CAMPAIGNING("정당/정치인 비하 및 선거운동");

    private String description;
}
