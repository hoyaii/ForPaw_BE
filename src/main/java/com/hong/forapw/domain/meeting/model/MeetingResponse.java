package com.hong.forapw.domain.meeting.model;

import java.time.LocalDateTime;
import java.util.List;

public class MeetingResponse {

    public record CreateMeetingDTO(Long id) {
    }

    public record FindMeetingByIdDTO(Long id,
                                     String name,
                                     LocalDateTime meetDate,
                                     String location,
                                     Long cost,
                                     Long participantNum,
                                     Integer maxNum,
                                     String organizer,
                                     String profileURL,
                                     String description,
                                     List<MeetingResponse.ParticipantDTO> participants) {
    }

    public record MeetingDTO(Long id,
                             String name,
                             LocalDateTime meetDate,
                             String location,
                             Long cost,
                             Long participantNum,
                             Integer maxNum,
                             String profileURL,
                             String description,
                             List<String> participants) {
    }

    public record FindMeetingListDTO(List<MeetingResponse.MeetingDTO> meetings) {
    }

    public record ParticipantDTO(String profileURL, String nickName) {
    }
}
