package com.hong.ForPaw.domain.Post;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Type {
    NOTICE("공지사항"),
    ADOPTION("입양 스토리"),
    PROTECTION("임시 보호 스토리"),
    QUESTION("궁금해요");

    private String value;
}