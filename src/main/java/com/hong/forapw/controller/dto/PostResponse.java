package com.hong.forapw.controller.dto;

import com.hong.forapw.domain.Province;

import java.time.LocalDateTime;
import java.util.List;

public class PostResponse {

    public record CreatePostDTO(Long id) {
    }

    public record CreateAnswerDTO(Long id) {
    }

    public record FindPostListDTO(List<PostDTO> posts, boolean isLastPage) {
    }

    public record FindMyPostListDTO(List<MyPostDTO> posts, boolean isLastPage) {
    }

    public record FindQnaListDTO(List<QnaDTO> questions, boolean isLastPage) {
    }

    public record FindMyCommentListDTO(List<MyCommentDTO> comments, boolean isLastPage) {
    }

    public record PostDTO(Long id,
                          String nickName,
                          String title,
                          String content,
                          LocalDateTime date,
                          Long commentNum,
                          Long likeNum,
                          String imageURL,
                          boolean isBlocked) {
    }

    public record QnaDTO(Long id,
                         String nickName,
                         String profileURL,
                         String title,
                         String content,
                         LocalDateTime date,
                         Long answerNum,
                         boolean isBlocked) {
    }

    public record MyPostDTO(Long id,
                            String nickName,
                            String title,
                            String content,
                            LocalDateTime date,
                            Long commentNum,
                            Long likeNum,
                            String imageURL,
                            boolean isBlocked,
                            String postType) {
    }

    public record PostImageDTO(Long id, String imageURL) {
    }

    public record FindPostByIdDTO(String nickName,
                                  String profileURL,
                                  String title,
                                  String content,
                                  LocalDateTime date,
                                  Long commentNum,
                                  Long likeNum,
                                  boolean isMine,
                                  boolean isLike,
                                  List<PostImageDTO> images,
                                  List<CommentDTO> comments) {
    }

    public record FindQnaByIdDTO(String nickName,
                                 String profileURL,
                                 String title,
                                 String content,
                                 LocalDateTime date,
                                 List<PostImageDTO> images,
                                 List<AnswerDTO> answers,
                                 boolean isMine) {
    }

    public record FindAnswerByIdDTO(String nickName,
                                    String content,
                                    LocalDateTime date,
                                    List<PostImageDTO> images,
                                    boolean isMine) {
    }

    public record AnswerDTO(Long id,
                            String nickName,
                            String profileURL,
                            String content,
                            LocalDateTime date,
                            List<PostImageDTO> images,
                            boolean isMine) {
    }

    public record CommentDTO(Long id,
                             String nickName,
                             String profileURL,
                             String content,
                             LocalDateTime date,
                             Province location,
                             Long likeNum,
                             boolean isLike,
                             List<ReplyDTO> replies) {
    }

    public record MyCommentDTO(Long commentId,
                               Long postId,
                               String postType,
                               String content,
                               LocalDateTime date,
                               String title,
                               Long commentNum,
                               boolean isBlocked) {
    }

    public record ReplyDTO(Long id,
                           String nickName,
                           String profileURL,
                           String replyName,
                           String content,
                           LocalDateTime date,
                           Province location,
                           Long likeNum,
                           boolean isLike) {
    }

    public record CreateCommentDTO(Long id) {
    }
}
