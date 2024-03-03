package com.hong.ForPaw.controller.DTO;

public class GroupRequest {

    public record CreateGroupDTO(String name, String region, String subRegion, String description,
                                 String category, String profileURL){}

    public record UpdateGroupDTO(String name, String region, String subRegion, String description,
                                 String category, String profileURL){}

    public record JoinGroupDTO(String greeting) {}
}
