package com.hong.ForPaw.controller.DTO;

import java.time.LocalDate;
import java.util.List;

public class AnimalResponse {

    public record FindAllAnimalsDTO(List<AnimalDTO> animalDTOS){ }

    public record AnimalDTO(Long id, String name, String age,
                            String gender, String specialMark, String region,
                            Integer inquiryNum, Integer likeNum, Boolean isLike,
                            String profileURL
    ){ };

    public record AnimalDetailDTO(Long id, String happenPlace, String kind, String color,
                                  String weight, LocalDate noticeSdt, LocalDate noticeEdt,
                                  String processState, String neuter){}
}
