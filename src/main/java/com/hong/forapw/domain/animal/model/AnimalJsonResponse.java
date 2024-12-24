package com.hong.forapw.domain.animal.model;

import com.hong.forapw.domain.shelter.Shelter;

public record AnimalJsonResponse(Shelter shelter, String animalJson) {
}
