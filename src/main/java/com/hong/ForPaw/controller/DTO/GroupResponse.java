package com.hong.ForPaw.controller.DTO;

import java.util.List;

public class GroupResponse {

    public record FindGroupByIdDTO(String name, String region, String subRegion, String description,
                                   String category, String profileURL) {}

    public record FindAllGroupDTO(List<RecommendGroupDTO> recommendGroupDTOS, List<NewGroupDTO> newGroupDTOS, List<LocalGroupDTO> localGroupDTOS, List<MyGroupDTO> myGroupDTOS) {}

    public record FindLocalGroupDTO(List<LocalGroupDTO> localGroupDTOS) {}

    public record FindNewGroupDTO(List<NewGroupDTO> newGroupDTOS) {}

    public record FindMyGroupDTO(List<MyGroupDTO> myGroupDTOS) {}

    public record RecommendGroupDTO(Long id, String name, String description, Integer participationNum, String category,
                                    String region, String subRegion, String profileURL, Integer likeNum) {}

    public record NewGroupDTO(Long id, String name, String category, String region,
                              String subRegion, String profileURL) {}

    public record LocalGroupDTO(Long id, String name, String description, Integer participationNum, String category,
                                String region, String subRegion, String profileURL, Integer likeNum) {}

    public record MyGroupDTO(Long id, String name, String description, Integer participationNum, String category,
                             String region, String subRegion, String profileURL, Integer likeNum) {}
}