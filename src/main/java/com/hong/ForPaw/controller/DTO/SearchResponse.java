package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.District;
import com.hong.ForPaw.domain.Post.PostType;
import com.hong.ForPaw.domain.Province;

import java.time.LocalDateTime;
import java.util.List;

public class SearchResponse {

    public record SearchAllDTO(List<ShelterDTO> shelters, List<PostDTO> posts, List<GroupDTO> groups) {}

    public record SearchShelterListDTO(List<ShelterDTO> shelters) {}

    public record SearchPostListDTO(List<PostDTO> posts) {}

    public record SearchGroupListDTO(List<GroupDTO> groups) {}

    public record ShelterDTO(Long id, String name) {}

    public record PostDTO(Long id,
                          PostType type,
                          String title,
                          String content,
                          LocalDateTime date,
                          String imageURL){}


    public record GroupDTO(Long id,
                           String name,
                           String description,
                           String category,
                           Province province,
                           District district,
                           String profileURL) {}

}
