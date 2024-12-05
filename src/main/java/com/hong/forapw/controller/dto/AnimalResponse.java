package com.hong.forapw.controller.dto;

import com.hong.forapw.domain.apply.ApplyStatus;

import java.time.LocalDate;
import java.util.List;

public class AnimalResponse {

    public record FindAnimalListDTO(List<AnimalDTO> animals, boolean isLastPage) {
    }

    public record FindRecommendedAnimalList(List<AnimalDTO> animals) {
    }

    public record FindLikeAnimalListDTO(List<AnimalDTO> animals) {
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
                            Boolean isLike,
                            String profileURL) {
    }

    public record FindAnimalByIdDTO(Long id,
                                    String name,
                                    String age,
                                    String gender,
                                    String specialMark,
                                    String region,
                                    Boolean isLike,
                                    String profileURL,
                                    String happenPlace,
                                    String kind,
                                    String color,
                                    String weight,
                                    LocalDate noticeSdt,
                                    LocalDate noticeEdt,
                                    String processState,
                                    String neuter,
                                    String introductionTitle,
                                    String introductionContent,
                                    boolean isAdopted) {
    }

    public record RecommendationDTO(List<Long> recommendedAnimals) {
    }
}
