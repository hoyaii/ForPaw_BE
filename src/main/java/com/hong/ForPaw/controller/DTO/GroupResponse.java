package com.hong.ForPaw.controller.DTO;

public class GroupResponse {

    public record FindGroupByIdDTO(String name, String region, String subRegion, String description,
                                   String category, String profileURL) {}
}
