package com.hong.ForPaw.controller.DTO;

import java.util.List;

public class ShelterResponse {

    public record FindShelterListDTO(List<ShelterDTO> shelters){}

    public record FindShelterInfoByIdDTO(Long id,
                                         Double lat,
                                         Double lng,
                                         String careAddr,
                                         String careTel,
                                         Long animalCnt) {}

    public record FindShelterAnimalsByIdDTO(List<AnimalDTO> animals) {}

    public record AnimalDTO(Long id,
                            String name,
                            String age,
                            String gender,
                            String specialMark,
                            String region,
                            Long inquiryNum,
                            Long likeNum,
                            Boolean isLike,
                            String profileURL
    ){};

    public record ShelterDTO(Long id,
                             String name,
                             Double lat,
                             Double lng,
                             String careAddr,
                             String careTel) {}
}
