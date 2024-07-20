package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.Apply.ApplyStatus;
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
            @NotNull(message = "정지 여부를 입력해주세요.")
            Boolean hasSuspension,
            @NotNull(message = "정지 기간을 입력해주세요.")
            Long suspensionDays){}
}
