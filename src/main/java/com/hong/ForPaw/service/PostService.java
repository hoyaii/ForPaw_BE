package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.PostRequest;
import com.hong.ForPaw.controller.DTO.PostResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Post.Comment;
import com.hong.ForPaw.domain.Post.Post;
import com.hong.ForPaw.domain.Post.PostImage;
import com.hong.ForPaw.domain.Post.Type;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.repository.CommentRepository;
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
    private final CommentRepository commentRepository;
    private final EntityManager entityManager;

    @Transactional
    public PostResponse.CreatePostDTO createPost(PostRequest.CreatePostDTO requestDTO, Long userId){

        User user = entityManager.getReference(User.class, userId);

        Post post = Post.builder()
                .user(user)
                .type(requestDTO.type())
                .title(requestDTO.title())
                .content(requestDTO.content())
                .build();

        List<PostImage> postImages = requestDTO.images().stream()
                .map(postImageDTO -> PostImage.builder()
                        .post(post)
                        .imageURL(postImageDTO.imageURL())
                        .build())
                .collect(Collectors.toList());

        postRepository.save(post);
        postImageRepository.saveAll(postImages);

        return new PostResponse.CreatePostDTO(post.getId());
    }

    @Transactional
    public PostResponse.FindPostListDTO findPostList(Type type, Pageable pageable){

        Page<Post> postPage = postRepository.findByType(type, pageable);

        List<PostResponse.PostDTO> postDTOS = postPage.stream()
                .map(post -> {
                    List<PostResponse.PostImageDTO> postImageDTOS = postImageRepository.findByPost(post).stream()
                            .map(postImage -> new PostResponse.PostImageDTO(postImage.getId(), postImage.getImageURL()))
                            .collect(Collectors.toList());

                    return new PostResponse.PostDTO(post.getId(), post.getTitle(), post.getContent(), post.getCreatedDate(), post.getCommentNum(), post.getLikeNum(), postImageDTOS);
                })
                .collect(Collectors.toList());

        return new PostResponse.FindPostListDTO(postDTOS);
    }

    @Transactional
    public PostResponse.FindPostByIdDTO findPostById(Long postId){

        Post post = postRepository.findById(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        List<Comment> comments = commentRepository.findByPost(post);
        List<PostResponse.CommentDTO> commentDTOS = comments.stream()
                .map(comment -> new PostResponse.CommentDTO(comment.getId(), comment.getUser().getNickName(), comment.getContent(), comment.getCreatedDate(), comment.getUser().getRegin()))
                .collect(Collectors.toList());

        return new PostResponse.FindPostByIdDTO(commentDTOS);
    }

    @Transactional
    public void updatePostById(PostRequest.UpdatePostDTO requestDTO, Long userId, Long postId){
        // 존재하지 않는 게시글이면 에러 발생
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        // 수정 권한 없음
        if(!post.getUser().getId().equals(userId)){
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }

        post.updatePost(requestDTO.title(), requestDTO.content());

        // 유지할 이미지를 제외한 모든 이미지 삭제
        if (requestDTO.retainedImageIds() != null && !requestDTO.retainedImageIds().isEmpty()) {
            postImageRepository.deleteByPostAndIdNotIn(post, requestDTO.retainedImageIds());
        } else {
            postImageRepository.deleteByPost(post);
        }

        // 새 이미지 추가
        List<PostImage> newImages = requestDTO.newImages().stream()
                .map(postImageDTO -> PostImage.builder()
                        .post(post)
                        .imageURL(postImageDTO.imageURL())
                        .build())
                .collect(Collectors.toList());

        postImageRepository.saveAll(newImages);
    }

    @Transactional
    public void createComment(PostRequest.CreateCommentDTO requestDTO, Long userId, Long postId){

        User user = entityManager.getReference(User.class, userId);
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new CustomException(ExceptionCode.POST_NOT_FOUND)
        );

        Comment comment = Comment.builder()
                .user(user)
                .post(post)
                .content(requestDTO.content())
                .build();

        commentRepository.save(comment);

        // 게시글의 댓글 수에 반영
        post.updateCommentNum(post.getCommentNum() + 1);

        // 알람 생성

    }
}