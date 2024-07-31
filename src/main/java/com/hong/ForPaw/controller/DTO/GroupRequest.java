package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.District;
import com.hong.ForPaw.domain.Group.GroupRole;
import com.hong.ForPaw.domain.Province;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
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
            Long maxNum){}

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
            Long maxNum){}

    public record ApproveJoinDTO(@NotNull(message = "id를 입력해주세요.") Long applicantId) {}

    public record RejectJoinDTO(@NotNull(message = "id를 입력해주세요.") Long applicantId) {}

    public record JoinGroupDTO(@NotBlank(message = "가입 인사말을 입력해주세요.") String greeting) {}

    public record UpdateUserRoleDTO(@NotNull(message = "id를 입력해주세요.") Long id, GroupRole role) {}

    public record CreateMeetingDTO(
            @NotBlank(message = "정기모임의 이름을 입력해주세요.")
            //@Pattern(regexp = "^[a-zA-Z0-9가-힣]+$", message = "모임 이름에는 띄어쓰기를 사용할 수 없습니다.")
            String name,
            @NotNull(message = "모임 날짜를 입력해주세요.")
            LocalDateTime meetDate,
            @NotBlank(message = "모임 장소를 입력해주세요.")
            String location,
            @NotNull(message = "모임 비용을 입력해주세요.")
            Long cost,
            @NotNull(message = "최대 인원수를 입력해주세요.")
            Integer maxNum,
            @NotBlank(message = "모임의 설명을 입력해주세요.")
            String description,
            @NotBlank
            String profileURL) {}

    public record UpdateMeetingDTO(
            @NotBlank(message = "정기모임의 이름을 입력해주세요.")
            //@Pattern(regexp = "^[a-zA-Z0-9가-힣]+$", message = "모임 이름에는 띄어쓰기를 사용할 수 없습니다.")
            String name,
            @NotNull(message = "모임 날짜를 입력해주세요.")
            LocalDateTime meetDate,
            @NotBlank(message = "모임 장소를 입력해주세요.")
            String location,
            @NotNull(message = "모임 비용을 입력해주세요.")
            Long cost,
            @NotNull(message = "최대 인원수를 입력해주세요.")
            Integer maxNum,
            @NotBlank(message = "모임의 설명을 입력해주세요.")
            String description,
            @NotBlank
            String profileURL) {}

    public record CreateNoticeDTO(
            @NotBlank(message = "제목을 입력해주세요.")
            String title,
            @NotBlank(message = "본문을 입력해주세요.")
            String content,
            List<PostRequest.PostImageDTO> images) {}
}