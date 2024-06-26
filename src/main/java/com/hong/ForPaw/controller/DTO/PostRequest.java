package com.hong.ForPaw.controller.DTO;


import com.hong.ForPaw.domain.Post.PostType;
import com.hong.ForPaw.domain.Report.ReportTargetType;
import com.hong.ForPaw.domain.Report.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PostRequest {

    public record CreatePostDTO(
            @NotBlank(message = "제목을 입력해주세요.")
            String title,
            @NotNull(message = "글의 종류를 선택해주세요")
            PostType type,
            @NotBlank(message = "본문을 입력해주세요.")
            String content,
            List<PostImageDTO> images) {}

    public record CreateAnswerDTO(@NotBlank(message = "답변을 입력해주세요.") String content, List<PostImageDTO> images) {}

    public record PostImageDTO(String imageURL) {}

    public record UpdatePostDTO(
            @NotBlank(message = "제목을 입력해주세요.")
            String title,
            @NotBlank(message = "본문을 입력해주세요.")
            String content,
            List<Long> retainedImageIds,
            List<PostImageDTO> newImages) {}

    public record CreateCommentDTO(@NotBlank(message = "댓글을 입력해주세요.") String content) {}

    public record UpdateCommentDTO(@NotBlank(message = "본문을 입력해주세요.") String content) {}

    public record SubmitReport(
            Long postId,
            Long commentId,
            @NotNull(message = "신고 유형을 선택해주세요.")
            ReportType type,
            @NotNull(message = "신고 하려는 컨텐츠의 유형을 선택해주세요.")
            ReportTargetType targetType,
            @NotBlank(message = "신고 사유를 입력해주세요.")
            String reason
    ) {}
}
