package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.PostRequest;
import com.hong.ForPaw.controller.DTO.PostResponse;
import com.hong.ForPaw.domain.Post.Post;
import com.hong.ForPaw.domain.Post.PostImage;
import com.hong.ForPaw.domain.Post.Type;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.repository.PostImageRepository;
import com.hong.ForPaw.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final EntityManager entityManager;

    @Transactional
    public void createPost(PostRequest.CreatePostDTO requestDTO, Long userId){

        User user = entityManager.getReference(User.class, userId);

        Post post = Post.builder()
                .user(user)
                .type(requestDTO.type())
                .title(requestDTO.title())
                .content(requestDTO.content())
                .build();

        List<PostImage> postImages = requestDTO.images().stream()
                .map(postImageDTO -> PostImage.builder().post(post).imageURL(postImageDTO.imageURL()).build())
                .collect(Collectors.toList());

        postRepository.save(post);
        postImageRepository.saveAll(postImages);
    }
}
