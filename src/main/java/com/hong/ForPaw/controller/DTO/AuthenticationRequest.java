package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.Apply.ApplyStatus;
import com.hong.ForPaw.domain.User.UserRole;

public class AuthenticationRequest {

    public record ChangeUserRoleDTO(Long userId, UserRole role){}

    public record SuspendUserDTO(Long userId, Long suspensionDays, String suspensionReason){};

    public record ChangeApplyStatusDTO(Long id, ApplyStatus status){}
}
