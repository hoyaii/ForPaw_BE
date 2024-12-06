package com.hong.forapw.controller.dto;

import com.hong.forapw.domain.Shelter;

public record AnimalJsonResponse(Shelter shelter, String animalJson) {
}
