package com.hong.forapw.domain.animal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AnimalType {

    DOG("강아지"),
    CAT("고양이"),
    OTHER("기타");

    private String value;
}
