package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.SearchResponse;
import com.hong.ForPaw.domain.Group.Group;
import com.hong.ForPaw.domain.Post.Post;
import com.hong.ForPaw.domain.Shelter;
import com.hong.ForPaw.repository.Group.GroupRepository;
import com.hong.ForPaw.repository.Post.PostRepository;
import com.hong.ForPaw.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final ShelterRepository shelterRepository;
    private final PostRepository postRepository;
    private final GroupRepository groupRepository;
    private final RedisService redisService;

    @Transactional
    public SearchResponse.SearchAllDTO searchAll(String keyword){
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);

        // 보호소 검색
        List<SearchResponse.ShelterDTO> shelterDTOS = getShelterDTOsByKeyword(formattedKeyword);

        // 게시글 검색
        List<SearchResponse.PostDTO> postDTOS = getPostDTOsByKeyword(formattedKeyword);

        // 그룹 검색
        List<SearchResponse.GroupDTO> groupDTOS = getGroupDTOsByKeyword(formattedKeyword);

        return new SearchResponse.SearchAllDTO(shelterDTOS, postDTOS, groupDTOS);
    }

    @Transactional
    public SearchResponse.SearchShelterListDTO searchShelterList(String keyword){
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);
        List<SearchResponse.ShelterDTO> shelterDTOS = getShelterDTOsByKeyword(formattedKeyword);

        return new SearchResponse.SearchShelterListDTO(shelterDTOS);
    }

    @Transactional
    public SearchResponse.SearchPostListDTO searchPostList(String keyword){
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);
        List<SearchResponse.PostDTO> postDTOS = getPostDTOsByKeyword(formattedKeyword);

        return new SearchResponse.SearchPostListDTO(postDTOS);
    }

    @Transactional
    public SearchResponse.SearchGroupListDTO searchGroupList(String keyword){
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);
        List<SearchResponse.GroupDTO> groupDTOS = getGroupDTOsByKeyword(formattedKeyword);

        return new SearchResponse.SearchGroupListDTO(groupDTOS);
    }

    private List<SearchResponse.ShelterDTO> getShelterDTOsByKeyword(String keyword){
        List<Shelter> shelters = shelterRepository.findByKeywordContaining(keyword);

        List<SearchResponse.ShelterDTO> shelterDTOS = shelters.stream()
                .filter(shelter -> shelter.getAnimalCnt() > 0)
                .map(shelter -> new SearchResponse.ShelterDTO(shelter.getId(), shelter.getName()))
                .collect(Collectors.toList());

        return shelterDTOS;
    }

    private List<SearchResponse.PostDTO> getPostDTOsByKeyword(String keyword){
        List<Post> posts = postRepository.findByTitleContaining(keyword);

        List<SearchResponse.PostDTO> postDTOS = posts.stream()
                .map(post -> {
                    List<SearchResponse.PostImageDTO> postImageDTOS = post.getPostImages().stream()
                            .map(postImage -> new SearchResponse.PostImageDTO(postImage.getId(), postImage.getImageURL()))
                            .collect(Collectors.toList());

                    Long likeNum = redisService.getDataInLong("postLikeNum", post.getId().toString());

                    return new SearchResponse.PostDTO(
                            post.getId(),
                            post.getTitle(),
                            post.getContent(),
                            post.getCreatedDate(),
                            post.getCommentNum(),
                            likeNum,
                            postImageDTOS);
                })
                .collect(Collectors.toList());

        return postDTOS;
    }

    private List<SearchResponse.GroupDTO> getGroupDTOsByKeyword(String keyword){
        List<Group> groups = groupRepository.findByNameContaining(keyword);

        List<SearchResponse.GroupDTO> groupDTOS = groups.stream()
                .map(group -> {
                    Long likeNum = redisService.getDataInLong("groupLikeNum", group.getId().toString());

                    return new SearchResponse.GroupDTO(
                        group.getId(),
                        group.getName(),
                        group.getDescription(), group.getParticipantNum(),
                        group.getCategory(),
                        group.getProvince(),
                        group.getDistrict(),
                        group.getProfileURL(),
                        likeNum);
                })
                .collect(Collectors.toList());

        return groupDTOS;
    }

    private String formatKeywordForFullTextSearch(String keyword){
        // 키워드를 스페이스로 분리하고 각 단어에 +와 *를 추가
        String[] words = keyword.split("\\s+");
        StringBuilder modifiedKeyword = new StringBuilder();

        for (String word : words) {
            modifiedKeyword.append("+").append(word).append("* ").append(" ");
        }

        return modifiedKeyword.toString().trim();
    }
}
