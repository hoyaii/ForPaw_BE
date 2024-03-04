package com.hong.ForPaw.controller.DTO;


import com.hong.ForPaw.domain.Post.Type;

import java.util.List;

public class PostRequest {

    public record CreatePostDTO(String title, Type type, String content, List<PostImageDTO> images) {}

    public record PostImageDTO(String imageURL) {}
}
