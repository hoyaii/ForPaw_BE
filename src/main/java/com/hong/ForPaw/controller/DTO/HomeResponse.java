package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.District;
import com.hong.ForPaw.domain.Province;

import java.time.LocalDateTime;
import java.util.List;

public class HomeResponse {

    public record FindHomeDTO(List<AnimalDTO> animals,
                              List<GroupDTO> groups,
                              List<PostDTO> posts) {}

    public record AnimalDTO(Long id,
                            String name,
                            String age,
                            String gender,
                            String specialMark,
                            String region,
                            Long inquiryNum,
                            Long likeNum,
                            String profileURL){}

    public record GroupDTO(Long id,
                                String name,
                                String description,
                                Long participationNum,
                                String category,
                                Province province,
                                District district,
                                String profileURL,
                                Long likeNum,
                                boolean isLike) {}

    public record PostDTO(Long id,
                          String name,
                          String title,
                          String content,
                          LocalDateTime date,
                          Long commentNum,
                          Long likeNum,
                          String imageURL){}
}
