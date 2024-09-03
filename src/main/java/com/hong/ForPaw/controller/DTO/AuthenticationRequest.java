package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.Apply.ApplyStatus;
import com.hong.ForPaw.domain.FAQ.FaqType;
import com.hong.ForPaw.domain.Report.ContentType;
import com.hong.ForPaw.domain.User.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AuthenticationRequest {

    public record ChangeUserRoleDTO(
            @NotNull(message = "변경하려는 유저의 ID를 입력해주세요.")
            Long userId,
            @NotNull(message = "변경하려는 역할을 입력해주세요.")
            UserRole role){}

    public record SuspendUserDTO(
            @NotNull(message = "정지하려는 유저의 ID를 입력해주세요.")
            Long userId,
            @NotNull(message = "정지 기간을 입력해주세요.")
            Long suspensionDays,
            @NotBlank(message = "정지 사유를 입력해주세요.")
            String suspensionReason){};

    public record ChangeApplyStatusDTO(
            @NotNull(message = "변경하려는 지원서의 ID를 입력해주세요.")
            Long id,
            @NotNull(message = "변경하려는 상태를 입력해주세요.")
            ApplyStatus status){}

    public record ProcessReportDTO(
            @NotNull(message = "신고 내역의 ID를 입력해주세요.")
            Long id,
            boolean hasSuspension,
            long suspensionDays,
            boolean hasBlocking){}

    public record AnswerInquiryDTO(
            @NotBlank(message = "답변 내용을 입력해주세요.")
            String content){}

    public record CreateFaqDTO(
            @NotBlank(message = "질문 내용을 입력해주세요.")
            String question,
            @NotBlank(message = "답변 내용을 입력해주세요.")
            String answer,
            @NotNull(message = "FAQ 타입을 입력해주세요.")
            FaqType type,
            boolean isTop){}
}
