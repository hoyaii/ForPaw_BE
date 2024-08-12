package com.hong.ForPaw.controller.DTO.Query;

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

