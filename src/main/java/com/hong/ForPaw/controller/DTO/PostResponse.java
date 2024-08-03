package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.Province;

import java.time.LocalDateTime;
import java.util.List;

public class PostResponse {

    public record CreatePostDTO(Long id) {}

    public record CreateAnswerDTO(Long id) {}

    public record FindAdoptionPostListDTO(List<PostDTO> adoptions) {}

    public record FindFosteringPostListDTO(List<PostDTO> fostering) {}

    public record FindQnaPostListDTO(List<QnaDTO> questions) {}

    public record PostDTO(Long id,
                          String nickName,
                          String title,
                          String content,
                          LocalDateTime date,
                          Long commentNum,
                          Long likeNum,
                          String imageURL){}

    public record QnaDTO(Long id,
                         String nickName,
                         String profileURL,
                         String title,
                         String content,
                         LocalDateTime date,
                         Long answerNum) {}

    public record PostImageDTO(Long id, String imageURL) {}

    public record FindPostByIdDTO(String nickName,
                                  String title,
                                  String content,
                                  LocalDateTime date,
                                  Long commentNum,
                                  Long likeNum,
                                  List<PostImageDTO> images,
                                  List<CommentDTO> comments,
                                  boolean isMine,
                                  boolean isLike) {}

    public record FindQnaByIdDTO(String nickName,
                                 String profileURL,
                                 String title,
                                 String content,
                                 LocalDateTime date,
                                 List<PostImageDTO> images,
                                 List<AnswerDTO> answers,
                                 boolean isMine) {}

    public record FindAnswerByIdDTO(String nickName,
                                  String content,
                                  LocalDateTime date,
                                  List<PostImageDTO> images,
                                  boolean isMine) {}

    public record AnswerDTO(Long id,
                            String nickName,
                            String profileURL,
                            String content,
                            LocalDateTime date,
                            List<PostImageDTO> images,
                            boolean isMine) {}

    public record CommentDTO(Long id,
                             String nickName,
                             String content,
                             LocalDateTime date,
                             Province location,
                             Long likeNum,
                             boolean isLike,
                             List<ReplyDTO> replies) {}

    public record ReplyDTO(Long id,
                           String nickName,
                           String replyName,
                           String content,
                           LocalDateTime date,
                           Province location,
                           Long likeNum,
                           boolean isLike) {}

    public record CreateCommentDTO(Long id) {}
}
