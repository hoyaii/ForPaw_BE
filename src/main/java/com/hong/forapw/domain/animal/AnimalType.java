package com.hong.forapw.domain.animal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AnimalType {

    DOG("강아지"),
    CAT("고양이"),
    OTHER("기타");

    private final String value;

    public static AnimalType from(String input) {
        if (input.startsWith("DOG")) {
            return DOG;
        } else if (input.startsWith("CAT")) {
            return CAT;
        } else {
            return OTHER;
        }
    }
}

