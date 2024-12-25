package com.hong.forapw.domain.search;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.region.constant.Province;

import java.time.LocalDateTime;
import java.util.List;

public class SearchResponse {

    public record SearchAllDTO(List<ShelterDTO> shelters, List<PostDTO> posts, List<GroupDTO> groups) {
    }

    public record SearchShelterListDTO(List<ShelterDTO> shelters) {
    }

    public record SearchPostListDTO(List<PostDTO> posts) {
    }

    public record SearchGroupListDTO(List<GroupDTO> groups) {
    }

    public record ShelterDTO(Long id, String name) {
    }

    public record PostDTO(Long id,
                          PostType type,
                          String title,
                          String content,
                          LocalDateTime date,
                          String imageURL,
                          String nickName,
                          Long commentNum,
                          Long likeNum) {
    }


    public record GroupDTO(Long id,
                           String name,
                           String description,
                           String category,
                           Province province,
                           District district,
                           String profileURL,
                           Long participantNum,
                           Long meetingNum) {
    }

}
