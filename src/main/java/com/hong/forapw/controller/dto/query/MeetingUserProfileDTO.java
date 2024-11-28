package com.hong.forapw.controller.dto.query;

import lombok.Getter;

@Getter
public class MeetingUserProfileDTO {
    private Long meetingId;
    private String profileURL;

    public MeetingUserProfileDTO(Long meetingId, String profileURL) {
        this.meetingId = meetingId;
        this.profileURL = profileURL;
    }
}

