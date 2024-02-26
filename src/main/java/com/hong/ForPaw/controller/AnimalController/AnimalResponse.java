package com.hong.ForPaw.controller.AnimalController;

import com.hong.ForPaw.domain.animal.Animal;

import java.util.ArrayList;
import java.util.List;

public class AnimalResponse {

    public record FindAllAnimalsDTO(List<AnimalDTO> animalDTOS){

        public record AnimalDTO(Long id,
                                String name,
                                String age,
                                String gender,
                                String specialMark,
                                String region,
                                Integer inquiryNum,
                                Integer likeNum,
                                Boolean isLike,
                                String profileURL
        ){ };
    }

}
