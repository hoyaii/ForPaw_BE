package com.hong.forapw.domain.shelter.model;

import java.util.List;
import java.util.Map;

public class ShelterResponse {

    public record FindShelterListDTO(List<ShelterDTO> shelters) {
    }

    public record FindShelterInfoByIdDTO(Long id,
                                         String name,
                                         Double lat,
                                         Double lng,
                                         String careAddr,
                                         String careTel,
                                         Long animalCnt) {
    }

    public record FindShelterAnimalsByIdDTO(List<AnimalDTO> animals, boolean isLastPage) {
    }

    public record FindShelterListWithAddr(Map<String, List<DistrictDTO>> province) {
    }

    public record DistrictDTO(Map<String, List<String>> district) {
    }

    public record AnimalDTO(Long id,
                            String name,
                            String age,
                            String gender,
                            String specialMark,
                            String kind,
                            String weight,
                            String neuter,
                            String processState,
                            String region,
                            Long inquiryNum,
                            Long likeNum,
                            boolean isLike,
                            String profileURL) {
    }

    public record ShelterDTO(Long id,
                             String name,
                             Double lat,
                             Double lng,
                             String careAddr,
                             String careTel) {
    }
}
