package com.hong.forapw.domain.meeting;

import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.meeting.entity.Meeting;
import com.hong.forapw.domain.meeting.model.MeetingRequest;
import com.hong.forapw.domain.meeting.model.MeetingResponse;
import com.hong.forapw.domain.user.entity.User;

import java.util.List;

public class MeetingMapper {

    private MeetingMapper() {
    }

    public static MeetingResponse.FindMeetingByIdDTO toFindMeetingByIdDTO(Meeting meeting, List<MeetingResponse.ParticipantDTO> participantDTOS) {
        return new MeetingResponse.FindMeetingByIdDTO(
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

    public static List<MeetingResponse.ParticipantDTO> toParticipantDTOs(List<User> participants) {
        return participants.stream()
                .map(user -> new MeetingResponse.ParticipantDTO(user.getProfileURL(), user.getNickname()))
                .toList();
    }

    public static Meeting buildMeeting(MeetingRequest.CreateMeetingDTO requestDTO, Group group, User creator) {
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

    public static MeetingResponse.MeetingDTO toMeetingDTO(Meeting meeting, List<String> participants) {
        return new MeetingResponse.MeetingDTO(
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
