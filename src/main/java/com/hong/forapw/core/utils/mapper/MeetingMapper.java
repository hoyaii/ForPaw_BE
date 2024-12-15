package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.GroupRequest;
import com.hong.forapw.controller.dto.GroupResponse;
import com.hong.forapw.domain.group.Group;
import com.hong.forapw.domain.group.Meeting;
import com.hong.forapw.domain.user.User;

import java.util.List;

public class MeetingMapper {

    private MeetingMapper() {
    }

    public static GroupResponse.FindMeetingByIdDTO toFindMeetingByIdDTO(Meeting meeting, List<GroupResponse.ParticipantDTO> participantDTOS) {
        return new GroupResponse.FindMeetingByIdDTO(
                meeting.getId(),
                meeting.getName(),
                meeting.getMeetDate(),
                meeting.getLocation(),
                meeting.getCost(),
                meeting.getParticipantNum(),
                meeting.getMaxNum(),
                meeting.getCreatorNickName(),
                meeting.getProfileURL(),
                meeting.getDescription(),
                participantDTOS);
    }

    public static List<GroupResponse.ParticipantDTO> toParticipantDTOs(List<User> participants) {
        return participants.stream()
                .map(user -> new GroupResponse.ParticipantDTO(user.getProfileURL(), user.getNickname()))
                .toList();
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

}
