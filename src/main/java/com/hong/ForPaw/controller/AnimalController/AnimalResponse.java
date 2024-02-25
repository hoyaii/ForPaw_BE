package com.hong.ForPaw.controller.AnimalController;

public class AnimalResponse {

    public record FindAllAnimalsDTO(){

        public record Animal(Long id,
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
