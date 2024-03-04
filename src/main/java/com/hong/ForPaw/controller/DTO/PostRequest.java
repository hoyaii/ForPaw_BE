package com.hong.ForPaw.controller.DTO;


import com.hong.ForPaw.domain.Post.Type;

import java.util.List;

public class PostRequest {

    public record CreatePostDTO(String title, Type type, String content, List<PostImageDTO> images) {}

    public record PostImageDTO(String imageURL) {}

    public record UpdatePostDTO(String title, String content, List<Long> retainedImageIds, List<PostImageDTO> newImages) {}

    public record CreateCommentDTO(String content) {}
}
