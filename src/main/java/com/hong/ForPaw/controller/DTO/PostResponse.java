package com.hong.ForPaw.controller.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class PostResponse {

    public record FindPostListDTO(List<PostDTO> posts){}

    public record PostDTO(Long id, String title, String content, LocalDateTime date, Integer commentNum, Integer likeNum, List<PostImageDTO> images){}

    public record PostImageDTO(String imageURL) {}
}
