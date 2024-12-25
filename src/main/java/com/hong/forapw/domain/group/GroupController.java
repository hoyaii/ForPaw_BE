package com.hong.forapw.domain.group;

import com.hong.forapw.domain.group.model.GroupRequest;
import com.hong.forapw.domain.group.model.GroupResponse;
import com.hong.forapw.security.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.group.service.GroupService;
import com.hong.forapw.domain.like.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class GroupController {

    private final GroupService groupService;
    private final LikeService likeService;
    private static final String SORT_BY_ID = "id";

    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(@RequestBody @Valid GroupRequest.CreateGroupDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponse.CreateGroupDTO responseDTO = groupService.createGroup(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<?> findGroupById(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponse.FindGroupByIdDTO responseDTO = groupService.findGroupById(groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/groups/{groupId}")
    public ResponseEntity<?> updateGroup(@RequestBody @Valid GroupRequest.UpdateGroupDTO requestDTO, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.updateGroup(requestDTO, groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/groups")
    public ResponseEntity<?> findGroups(@AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponse.FindAllGroupListDTO responseDTO = groupService.findGroups(getUserIdSafely(userDetails));
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/local")
    public ResponseEntity<?> findLocalGroups(@RequestParam Province province, @RequestParam District district,
                                             @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Long> likedGroupList = groupService.getLikedGroupList(getUserIdSafely(userDetails));
        List<GroupResponse.LocalGroupDTO> localGroupDTOS = groupService.findLocalGroups(getUserIdSafely(userDetails), province, district, likedGroupList, pageable);
        GroupResponse.FindLocalGroupListDTO responseDTO = new GroupResponse.FindLocalGroupListDTO(localGroupDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/new")
    public ResponseEntity<?> findNewGroups(@RequestParam(value = "province", required = false) Province province,
                                           @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<GroupResponse.NewGroupDTO> newGroupDTOS = groupService.findNewGroups(getUserIdSafely(userDetails), province, pageable);
        GroupResponse.FindNewGroupListDTO responseDTO = new GroupResponse.FindNewGroupListDTO(newGroupDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/localAndNew")
    public ResponseEntity<?> findLocalAndNewGroups(@RequestParam Province province, @RequestParam District district,
                                                   @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Long> likedGroupList = groupService.getLikedGroupList(getUserIdSafely(userDetails));
        GroupResponse.FindLocalAndNewGroupListDTO responseDTO = groupService.findLocalAndNewGroups(getUserIdSafely(userDetails), province, district, likedGroupList, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/my")
    public ResponseEntity<?> findMyGroups(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Long> likedGroupList = groupService.getLikedGroupList(getUserIdSafely(userDetails));
        List<GroupResponse.MyGroupDTO> myGroupDTOS = groupService.findMyGroups(getUserIdSafely(userDetails), likedGroupList, pageable);
        GroupResponse.FindMyGroupListDTO responseDTO = new GroupResponse.FindMyGroupListDTO(myGroupDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}/detail")
    public ResponseEntity<?> findGroupDetailById(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponse.FindGroupDetailByIdDTO responseDTO = groupService.findGroupDetailById(getUserIdSafely(userDetails), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/groups/{groupId}/notices")
    public ResponseEntity<?> findNotices(@PathVariable Long groupId,
                                         @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.checkGroupAndIsMember(groupId, userDetails.getUser().getId());
        List<GroupResponse.NoticeDTO> noticeDTOS = groupService.findNotices(userDetails.getUser().getId(), groupId, pageable);
        GroupResponse.FindNoticeListDTO responseDTO = new GroupResponse.FindNoticeListDTO(noticeDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/groups/{groupId}/join")
    public ResponseEntity<?> joinGroup(@RequestBody @Valid GroupRequest.JoinGroupDTO requestDTO, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.joinGroup(requestDTO, userDetails.getUser().getId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/withdraw")
    public ResponseEntity<?> withdrawGroup(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.withdrawGroup(userDetails.getUser().getId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/expel")
    public ResponseEntity<?> expelGroupMember(@RequestBody @Valid GroupRequest.ExpelGroupMember requestDTO, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.expelGroupMember(userDetails.getUser().getId(), requestDTO.userId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/groups/{groupId}/join")
    public ResponseEntity<?> findApplicants(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponse.FindApplicantListDTO responseDTO = groupService.findApplicants(userDetails.getUser().getId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/groups/{groupID}/join/approve")
    public ResponseEntity<?> approveJoin(@RequestBody @Valid GroupRequest.ApproveJoinDTO requestDTO, @PathVariable Long groupID, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.approveJoin(userDetails.getUser().getId(), requestDTO.applicantId(), groupID);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupID}/join/reject")
    public ResponseEntity<?> rejectJoin(@RequestBody @Valid GroupRequest.RejectJoinDTO requestDTO, @PathVariable Long groupID, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.rejectJoin(userDetails.getUser().getId(), requestDTO.applicantId(), groupID);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/groups/{groupId}/notices")
    public ResponseEntity<?> createNotice(@RequestBody @Valid GroupRequest.CreateNoticeDTO requestDTO, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponse.CreateNoticeDTO responseDTO = groupService.createNotice(requestDTO, userDetails.getUser().getId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/groups/{groupId}/like")
    public ResponseEntity<?> likeGroup(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        likeService.likeGroup(groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.deleteGroup(groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PatchMapping("/groups/{groupId}/userRole")
    public ResponseEntity<?> updateUserRole(@RequestBody @Valid GroupRequest.UpdateUserRoleDTO requestDTO, @PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.updateUserRole(requestDTO, groupId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<?> findGroupMembers(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        GroupResponse.FindGroupMemberListDTO responseDTO = groupService.findGroupMembers(userDetails.getUser().getId(), groupId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    // 로그인 되지 않았을 때, NullException 방지하면서 userId를 null로 처리하기 위한 메서드
    private Long getUserIdSafely(CustomUserDetails userDetails) {
        return Optional.ofNullable(userDetails)
                .map(CustomUserDetails::getUser)
                .map(User::getId)
                .orElse(null);
    }
}