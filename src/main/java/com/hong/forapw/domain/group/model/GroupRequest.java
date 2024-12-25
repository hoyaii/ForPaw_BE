package com.hong.forapw.domain.group.model;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.post.model.PostRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class GroupRequest {

    public record CreateGroupDTO(
            @NotBlank(message = "그룹의 이름을 입력해주세요.")
            //@Pattern(regexp = "^[a-zA-Z0-9가-힣]+$", message = "그룹 이름에는 띄어쓰기를 사용할 수 없습니다.")
            String name,
            @NotNull(message = "활동 지역을 입력해주세요.")
            Province province,
            @NotNull(message = "활동 지역을 입력해주세요.")
            District district,
            String subDistrict,
            @NotBlank(message = "그룹의 설명을 입력해주세요.")
            String description,
            String category,
            @NotBlank
            String profileURL,
            Long maxNum,
            boolean isShelterOwns,
            String shelterName) {
    }

    public record UpdateGroupDTO(
            @NotBlank(message = "그룹의 이름을 입력해주세요.")
            //@Pattern(regexp = "^[a-zA-Z0-9가-힣]+$", message = "그룹 이름에는 띄어쓰기를 사용할 수 없습니다.")
            String name,
            @NotNull(message = "활동 지역을 입력해주세요.")
            Province province,
            @NotNull(message = "활동 지역을 입력해주세요.")
            District district,
            String subDistrict,
            @NotBlank(message = "그룹의 설명을 입력해주세요.")
            String description,
            String category,
            @NotBlank
            String profileURL,
            Long maxNum) {
    }

    public record ApproveJoinDTO(@NotNull(message = "id를 입력해주세요.") Long applicantId) {
    }

    public record RejectJoinDTO(@NotNull(message = "id를 입력해주세요.") Long applicantId) {
    }

    public record JoinGroupDTO(@NotBlank(message = "가입 인사말을 입력해주세요.") String greeting) {
    }

    public record UpdateUserRoleDTO(@NotNull(message = "id를 입력해주세요.") Long userId, GroupRole role) {
    }

    public record CreateNoticeDTO(
            @NotBlank(message = "제목을 입력해주세요.")
            String title,
            @NotBlank(message = "본문을 입력해주세요.")
            String content,
            List<PostRequest.PostImageDTO> images) {
    }

    public record ExpelGroupMember(@NotNull(message = "강퇴할 유저 ID를 입력해주세요.") Long userId) {
    }
}