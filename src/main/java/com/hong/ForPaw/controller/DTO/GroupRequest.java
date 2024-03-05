package com.hong.ForPaw.controller.DTO;

import java.time.LocalDateTime;

public class GroupRequest {

    public record CreateGroupDTO(String name, String region, String subRegion, String description,
                                 String category, String profileURL){}

    public record UpdateGroupDTO(String name, String region, String subRegion, String description,
                                 String category, String profileURL){}

    public record JoinGroupDTO(String greeting) {}

    public record CreateMeetingDTO(String name, LocalDateTime date, String location, Long cost,
                                   Integer maxNum, String description, String profileURL) {}

    public record UpdateMeetingDTO(String name, LocalDateTime date, String location, Long cost,
                                   Integer maxNum, String description, String profileURL) {}
}