package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.Apply.ApplyStatus;

import java.time.LocalDate;
import java.util.List;

public class AnimalResponse {

    public record FindAnimalListDTO(List<AnimalDTO> animals, boolean isLastPage) {}

    public record FindRecommendedAnimalList(List<AnimalDTO> animals) {}

    public record FindLikeAnimalListDTO(List<AnimalDTO> animals) {}

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
                            String profileURL){};

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
                                    String introductionContent){}

    public record FindApplyListDTO(List<ApplyDTO> applies) { }

    public record CreateApplyDTO(Long id) {}

    public record ApplyDTO(Long id,
                           String animalName,
                           String kind,
                           String gender,
                           String age,
                           String userName,
                           String tel,
                           String residence,
                           ApplyStatus status){ }

    public record RecommendationDTO(List<Long> recommendedAnimals) {}
}
