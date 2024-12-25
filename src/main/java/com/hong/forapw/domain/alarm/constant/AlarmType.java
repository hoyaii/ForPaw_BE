package com.hong.forapw.domain.alarm.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AlarmType {
    COMMENT("새 댓글"),
    NOTICE("공지사항"),
    CHATTING("새 채팅"),
    ANSWER("궁금해요 답변"),
    NEW_MEETING("새로운 정기모임"),
    TODAY_MEETING("오늘의 정기모임"),
    JOIN("그룹 가입");

    private String value;
}
