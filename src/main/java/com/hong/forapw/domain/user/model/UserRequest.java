package com.hong.forapw.domain.user.model;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.user.constant.AuthProvider;
import com.hong.forapw.domain.user.constant.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRequest {

    public record LoginDTO(
            @NotBlank(message = "이메일을 입력해주세요.")
            @Pattern(regexp = "^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "올바른 이메일 형식을 입력해주세요.")
            String email,

            @NotBlank(message = "비밀번호를 입력해주세요.")
            @Size(min = 8, max = 20, message = "비밀번호는 8자에서 20자 이내여야 합니다.")
            @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@#$%^&+=!~`<>,./?;:'\"\\[\\]{}\\\\()|_-])\\S*$", message = "올바른 비밀번호 형식을 입력해주세요.")
            String password) {
    }

    public record EmailDTO(
            @NotBlank(message = "이메일을 입력해주세요.")
            @Pattern(regexp = "^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "올바른 이메일 형식을 입력해주세요.")
            String email) {
    }

    public record CheckNickDTO(
            @NotBlank(message = "닉네임을 입력해주세요.")
            String nickName) {
    }

    public record VerifyCodeDTO(
            @NotBlank
            @Pattern(regexp = "^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "올바른 이메일 형식을 입력해주세요.")
            String email,
            @NotBlank(message = "코드를 입력해주세요.")
            String code) {
    }

    public record ResetPasswordDTO(
            @NotBlank(message = "코드를 입력해주세요.")
            String code,
            @NotBlank(message = "비밀번호를 입력해주세요.")
            @Size(min = 8, max = 20, message = "비밀번호는 8자에서 20자 이내여야 합니다.")
            @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@#$%^&+=!~`<>,./?;:'\"\\[\\]{}\\\\()|_-])\\S*$", message = "올바른 비밀번호 형식을 입력해주세요.")
            String newPassword
    ) {
    }

    public record JoinDTO(
            @NotBlank(message = "이메일을 입력해주세요.")
            @Pattern(regexp = "^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "올바른 이메일 형식을 입력해주세요.")
            String email,
            @NotBlank(message = "이름을 입력해주세요.")
            String name,
            @NotBlank(message = "닉네을 입력해주세요.")
            @Size(min = 2, max = 20, message = "닉네임은 2자에서 20자 이내여야 합니다.")
            String nickName,
            @NotNull(message = "활동 지역을 입력해주세요.")
            Province province,
            @NotNull(message = "활동 지역을 입력해주세요.")
            District district,
            String subDistrict,
            @NotBlank(message = "비밀번호를 입력해주세요.")
            @Size(min = 8, max = 20, message = "비밀번호는 8자에서 20자 이내여야 합니다.")
            @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@#$%^&+=!~`<>,./?;:'\"\\[\\]{}\\\\()|_-])\\S*$", message = "올바른 비밀번호 형식을 입력해주세요.")
            String password,
            @NotBlank(message = "비밀번호를 입력해주세요.")
            String passwordConfirm,
            String profileURL,
            boolean isShelterOwns,
            boolean isMarketingAgreed) {
    }

    public record SocialJoinDTO(
            @NotBlank(message = "이메일을 입력해주세요.")
            @Pattern(regexp = "^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "올바른 이메일 형식을 입력해주세요.")
            String email,
            @NotBlank(message = "이름을 입력해주세요.")
            String name,
            @NotBlank(message = "닉네을 입력해주세요.")
            @Size(min = 2, max = 20, message = "닉네임은 2자에서 20자 이내여야 합니다.")
            String nickName,
            @NotNull(message = "활동 지역을 입력해주세요.")
            Province province,
            @NotNull(message = "활동 지역을 입력해주세요.")
            District district,
            String subDistrict,
            @NotBlank(message = "프로필을 입력해주세요.")
            String profileURL,
            @NotNull(message = "가입 경로를 입력해주세요.")
            AuthProvider authProvider,
            boolean isShelterOwns,
            boolean isMarketingAgreed) {
    }

    public record CurPasswordDTO(@NotBlank String password) {
    }

    public record UpdatePasswordDTO(
            @NotBlank(message = "비밀번호를 입력해주세요.")
            @Size(min = 8, max = 20, message = "비밀번호는 8자에서 20자 이내여야 합니다.")
            @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@#$%^&+=!~`<>,./?;:'\"\\[\\]{}\\\\()|_-])\\S*$", message = "올바른 비밀번호 형식을 입력해주세요.")
            String newPassword,
            @NotBlank(message = "비밀번호를 입력해주세요.")
            String newPasswordConfirm,
            @NotBlank(message = "현재 비밀번호를 입력해주세요.")
            String curPassword) {
    }

    public record UpdateProfileDTO(
            @NotBlank(message = "닉네임을 입력해주세요.")
            String nickName,
            @NotNull(message = "활동 지역을 입력해주세요.")
            Province province,
            @NotNull(message = "활동 지역을 입력해주세요.")
            District district,
            String subDistrict,
            @NotBlank(message = "프로필을 입력해주세요.")
            String profileURL) {
    }

    public record UpdateAccessTokenDTO(@NotBlank(message = "토큰이 존재하지 않습니다.") String refreshToken) {
    }

    public record UpdateRoleDTO(Long userId, @NotNull(message = "역할을 선택해주세요.") UserRole role) {
    }

    public record ValidateAccessTokenDTO(
            @NotNull(message = "userId를 입력해주세요.") Long userId,
            @NotBlank(message = "토큰이 존재하지 않습니다.") String accessToken) {
    }
}