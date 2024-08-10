package com.hong.ForPaw.controller.DTO;


import com.hong.ForPaw.domain.Post.PostType;
import com.hong.ForPaw.domain.Report.ContentType;
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
            @NotNull(message = "이미지가 비어있다면, null이 아닌 빈 리스트로 보내주세요.")
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
            @NotNull(message = "신고 하려는 컨텐츠의 유형을 선택해주세요.")
            ContentType contentType,
            @NotNull(message = "신고 하려는 컨텐츠의 ID를 입력해주세요.")
            Long contentId,
            @NotNull(message = "신고 유형을 선택해주세요.")
            ReportType reportType,
            @NotBlank(message = "신고 사유를 입력해주세요.")
            String reason
    ) {}
}
