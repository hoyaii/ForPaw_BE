package com.hong.forapw.domain.post.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PostType {

    NOTICE("공지사항"),
    ADOPTION("입양 스토리"),
    FOSTERING("임시 보호 스토리"),
    QUESTION("궁금해요"),
    ANSWER("답변");

    private final String value;

    public boolean isImageRequired() {
        return this != QUESTION;
    }

    public boolean isQuestion() {
        return this == QUESTION;
    }
}