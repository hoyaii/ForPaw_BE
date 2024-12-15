package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.GroupResponse;
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
}
