package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.GroupRequest;
import com.hong.forapw.controller.dto.GroupResponse;
import com.hong.forapw.domain.group.Group;
import com.hong.forapw.domain.group.GroupUser;
import com.hong.forapw.domain.group.Meeting;
import com.hong.forapw.domain.post.Post;
import com.hong.forapw.domain.post.PostType;
import com.hong.forapw.domain.user.User;

import java.util.List;

public class GroupMapper {

    private GroupMapper() {
    }

    public static Group buildGroup(GroupRequest.CreateGroupDTO requestDTO) {
        return Group.builder()
                .name(requestDTO.name())
                .province(requestDTO.province())
                .district(requestDTO.district())
                .subDistrict(requestDTO.subDistrict())
                .description(requestDTO.description())
                .category(requestDTO.category())
                .profileURL(requestDTO.profileURL())
                .maxNum(requestDTO.maxNum())
                .isShelterOwns(requestDTO.isShelterOwns())
                .shelterName(requestDTO.shelterName())
                .build();
    }

    public static GroupResponse.FindGroupByIdDTO toFindGroupByIdDTO(Group group) {
        return new GroupResponse.FindGroupByIdDTO(
                group.getName(),
                group.getProvince(),
                group.getDistrict(),
                group.getSubDistrict(),
                group.getDescription(),
                group.getCategory(),
                group.getProfileURL(),
                group.getMaxNum()
        );
    }

    public static GroupResponse.LocalGroupDTO toLocalGroupDTO(Group group, Long likeNum, boolean isLikedGroup) {
        return new GroupResponse.LocalGroupDTO(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getParticipantNum(),
                group.getCategory(),
                group.getProvince(),
                group.getDistrict(),
                group.getProfileURL(),
                likeNum,
                isLikedGroup,
                group.isShelterOwns(),
                group.getShelterName());
    }

    public static GroupResponse.NewGroupDTO toNewGroupDTO(Group group) {
        return new GroupResponse.NewGroupDTO(
                group.getId(),
                group.getName(),
                group.getCategory(),
                group.getProvince(),
                group.getDistrict(),
                group.getProfileURL());
    }

    public static GroupResponse.MyGroupDTO toMyGroupDTO(Group group, Long likeNum, boolean isLikedGroup) {
        return new GroupResponse.MyGroupDTO(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getParticipantNum(),
                group.getCategory(),
                group.getProvince(),
                group.getDistrict(),
                group.getProfileURL(),
                likeNum,
                isLikedGroup,
                group.isShelterOwns(),
                group.getShelterName());
    }

    public static GroupResponse.NoticeDTO toNoticeDTO(Post notice, boolean isRead) {
        return new GroupResponse.NoticeDTO(
                notice.getId(),
                notice.getWriterNickName(),
                notice.getCreatedDate(),
                notice.getTitle(),
                isRead);
    }

    public static GroupResponse.MeetingDTO toMeetingDTO(Meeting meeting, List<String> participants) {
        return new GroupResponse.MeetingDTO(
                meeting.getId(),
                meeting.getName(),
                meeting.getMeetDate(),
                meeting.getLocation(),
                meeting.getCost(),
                meeting.getParticipantNum(),
                meeting.getMaxNum(),
                meeting.getProfileURL(),
                meeting.getDescription(),
                participants);
    }

    public static GroupResponse.MemberDetailDTO toMemberDetailDTO(GroupUser groupUser) {
        return new GroupResponse.MemberDetailDTO(
                groupUser.getUserId(),
                groupUser.getUserNickname(),
                groupUser.getUserProfileURL(),
                groupUser.getCreatedDate(),
                groupUser.getGroupRole()
        );
    }

    public static GroupResponse.ApplicantDTO toApplicantDTO(GroupUser groupUser) {
        return new GroupResponse.ApplicantDTO(
                groupUser.getUserId(),
                groupUser.getUserNickname(),
                groupUser.getGreeting(),
                groupUser.getUserEmail(),
                groupUser.getUserProfileURL(),
                groupUser.getUserProvince(),
                groupUser.getUserDistrict(),
                groupUser.getCreatedDate());
    }

    public static Meeting buildMeeting(GroupRequest.CreateMeetingDTO requestDTO, Group group, User creator) {
        return Meeting.builder()
                .group(group)
                .creator(creator)
                .name(requestDTO.name())
                .meetDate(requestDTO.meetDate())
                .location(requestDTO.location())
                .cost(requestDTO.cost())
                .maxNum(requestDTO.maxNum())
                .description(requestDTO.description())
                .profileURL(requestDTO.profileURL())
                .build();
    }

    public static GroupResponse.RecommendGroupDTO toRecommendGroupDTO(Group group, Long likeNum, boolean isLike) {
        return new GroupResponse.RecommendGroupDTO(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getParticipantNum(),
                group.getCategory(),
                group.getProvince(),
                group.getDistrict(),
                group.getProfileURL(),
                likeNum,
                isLike,
                group.isShelterOwns(),
                group.getShelterName());
    }

    public static GroupResponse.MemberDTO toMemberDTO(GroupUser groupUser) {
        return new GroupResponse.MemberDTO(
                groupUser.getUserId(),
                groupUser.getUserNickname(),
                groupUser.getGroupRole(),
                groupUser.getUserProfileURL(),
                groupUser.getCreatedDate());
    }

    public static Post buildNotice(GroupRequest.CreateNoticeDTO requestDTO, User noticer, Group group) {
        return Post.builder()
                .user(noticer)
                .group(group)
                .postType(PostType.NOTICE)
                .title(requestDTO.title())
                .content(requestDTO.content())
                .build();
    }
}
