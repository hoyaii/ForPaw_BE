package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.HomeResponse;
import com.hong.forapw.domain.animal.Animal;
import com.hong.forapw.domain.post.Post;

public class HomeMapper {

    private HomeMapper() {
    }

    public static HomeResponse.AnimalDTO toAnimalDTO(Animal animal, Long likeNum) {
        return new HomeResponse.AnimalDTO(
                animal.getId(),
                animal.getName(),
                animal.getAge(),
                animal.getGender(),
                animal.getSpecialMark(),
                animal.getRegion(),
                animal.getInquiryNum(),
                likeNum,
                animal.getProfileURL());
    }

    public static HomeResponse.PostDTO toPostDTO(Post post, Long likeNum, String imageURL) {
        return new HomeResponse.PostDTO(
                post.getId(),
                post.getWriterNickName(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedDate(),
                post.getCommentNum(),
                likeNum,
                imageURL,
                post.getPostType());
    }
}
