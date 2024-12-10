package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.PostRequest;
import com.hong.forapw.controller.dto.PostResponse;
import com.hong.forapw.domain.post.Comment;
import com.hong.forapw.domain.post.Post;
import com.hong.forapw.domain.post.PostImage;

import java.util.ArrayList;
import java.util.List;

public class PostMapper {

    private PostMapper() {
    }

    public static List<PostResponse.AnswerDTO> toAnswerDTOs(List<Post> answers, Long userId) {
        return answers.stream()
                .map(answer -> new PostResponse.AnswerDTO(
                        answer.getId(),
                        answer.getWriterNickName(),
                        answer.getWriterProfileURL(),
                        answer.getContent(),
                        answer.getCreatedDate(),
                        toPostImageDTOs(answer),
                        answer.isOwner(userId)))
                .toList();
    }

    public static List<PostResponse.PostImageDTO> toPostImageDTOs(Post post) {
        return post.getPostImages().stream()
                .map(postImage -> new PostResponse.PostImageDTO(postImage.getId(), postImage.getImageURL()))
                .toList();
    }

    public static List<PostImage> toPostImages(List<PostRequest.PostImageDTO> imageDTOs) {
        return imageDTOs.stream()
                .map(postImageDTO -> PostImage.builder()
                        .imageURL(postImageDTO.imageURL())
                        .build())
                .toList();
    }

    public static PostResponse.PostDTO toPostDTO(Post post, Long cachedPostLikeNum) {
        return new PostResponse.PostDTO(
                post.getId(),
                post.getWriterNickName(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedDate(),
                post.getCommentNum(),
                cachedPostLikeNum,
                post.getFirstImageURL(),
                post.isBlocked()
        );
    }

    public static PostResponse.QnaDTO toQnaDTO(Post post) {
        return new PostResponse.QnaDTO(
                post.getId(),
                post.getWriterNickName(),
                post.getWriterProfileURL(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedDate(),
                post.getAnswerNum(),
                post.isBlocked()
        );
    }

    public static PostResponse.MyPostDTO toMyPostDTO(Post post, Long cachedPostLikeNum) {
        return new PostResponse.MyPostDTO(
                post.getId(),
                post.getWriterNickName(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedDate(),
                post.getCommentNum(),
                cachedPostLikeNum,
                post.getFirstImageURL(),
                post.isBlocked(),
                post.getPostTypeString()
        );
    }

    public static PostResponse.MyCommentDTO toMyCommentDTO(Comment comment) {
        return new PostResponse.MyCommentDTO(
                comment.getId(),
                comment.getPostId(),
                comment.getPostTypeValue(),
                comment.getContent(),
                comment.getCreatedDate(),
                comment.getPostTitle(),
                comment.getPostCommentNumber(),
                comment.isPostBlocked()
        );
    }

    public static PostResponse.CommentDTO toParentCommentDTO(Comment comment, Long likeCount, boolean isLikedByUser) {
        return new PostResponse.CommentDTO(
                comment.getId(),
                comment.getWriterNickname(),
                comment.getWriterProfileURL(),
                comment.getContent(),
                comment.getCreatedDate(),
                comment.getWriterProvince(),
                likeCount,
                isLikedByUser,
                new ArrayList<>() // 답변을 담을 리스트
        );
    }

    public static PostResponse.ReplyDTO toReplyDTO(Comment childComment, boolean isLikedByUser, Long likeCount) {
        return new PostResponse.ReplyDTO(
                childComment.getId(),
                childComment.getWriterNickname(),
                childComment.getWriterProfileURL(),
                childComment.getParentWriterNickname(),
                childComment.getContent(),
                childComment.getCreatedDate(),
                childComment.getWriterProvince(),
                likeCount,
                isLikedByUser
        );
    }
}
