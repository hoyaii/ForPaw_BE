package com.hong.ForPaw.controller.DTO;

public class GroupRequest {

    public record createGroupDTO(String name, String region, String subRegion, String description,
                                 String category, String profileURL){}
}
