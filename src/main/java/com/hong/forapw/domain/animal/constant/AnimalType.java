package com.hong.forapw.domain.animal.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AnimalType {

    DOG("강아지"),
    CAT("고양이"),
    OTHER("기타");

    private final String value;

    public static AnimalType fromPrefix(String prefix) {
        if (prefix.startsWith("DOG")) {
            return DOG;
        } else if (prefix.startsWith("CAT")) {
            return CAT;
        } else {
            return OTHER;
        }
    }

    public static AnimalType fromString(String string) {
        if ("DOG".equalsIgnoreCase(string)) {
            return DOG;
        } else if ("CAT".equalsIgnoreCase(string)) {
            return CAT;
        } else if ("OTHER".equalsIgnoreCase(string)) {
            return OTHER;
        } else {
            return null;
        }
    }
}

