package com.hong.forapw.controller.dto;

import com.hong.forapw.domain.District;
import com.hong.forapw.domain.post.PostType;
import com.hong.forapw.domain.Province;

import java.time.LocalDateTime;
import java.util.List;

public class HomeResponse {

    public record FindHomeDTO(List<AnimalDTO> animals,
                              List<GroupResponse.RecommendGroupDTO> groups,
                              List<PostDTO> posts) {
    }

    public record AnimalDTO(Long id,
                            String name,
                            String age,
                            String gender,
                            String specialMark,
                            String region,
                            Long inquiryNum,
                            Long likeNum,
                            String profileURL) {
    }

    public record GroupDTO(Long id,
                           String name,
                           String description,
                           Long participationNum,
                           String category,
                           Province province,
                           District district,
                           String profileURL,
                           Long likeNum,
                           boolean isLike) {
    }

    public record PostDTO(Long id,
                          String name,
                          String title,
                          String content,
                          LocalDateTime date,
                          Long commentNum,
                          Long likeNum,
                          String imageURL,
                          PostType postType) {
    }
}
