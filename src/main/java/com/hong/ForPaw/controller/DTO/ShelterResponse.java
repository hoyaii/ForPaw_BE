package com.hong.ForPaw.controller.DTO;

import java.util.List;

public class ShelterResponse {

    public record FindAllSheltersDTO(List<ShelterDTO> shelterDTOS){}

    public record ShelterDTO(Long id, String name, String careAddr, String careTel) {}
}
