package com.hong.ForPaw.domain.Animal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AnimalType {

    dog("강아지"),
    cat("고양이"),
    other("기타");

    private String value;
}
