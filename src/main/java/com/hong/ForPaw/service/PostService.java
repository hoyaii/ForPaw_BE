package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.PostRequest;
import com.hong.ForPaw.domain.Post.Post;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.repository.PostRepository;
import com.hong.ForPaw.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import javax.persistence.EntityManager;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final EntityManager entityManager;

    @Transactional
    public void createPost(@RequestBody PostRequest.CreatePostDTO requestDTO, Long userId){

        User user = entityManager.getReference(User.class, userId);

        Post post = Post.builder()
                .user(user)
                .type(requestDTO.type())
                .title(requestDTO.title())
                .content(requestDTO.content())
                .build();

        postRepository.save(post);
    }
}
