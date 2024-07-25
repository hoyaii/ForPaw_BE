package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.SearchResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Group.Group;
import com.hong.ForPaw.domain.Post.Post;
import com.hong.ForPaw.domain.Shelter;
import com.hong.ForPaw.repository.Group.GroupRepository;
import com.hong.ForPaw.repository.Post.PostRepository;
import com.hong.ForPaw.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    @Transactional(readOnly = true)
    public SearchResponse.SearchAllDTO searchAll(String keyword, Pageable pageable){
        checkKeywordEmpty(keyword);

        // 보호소 검색
        List<SearchResponse.ShelterDTO> shelterDTOS = searchShelterList(keyword, pageable);

        // 게시글 검색
        List<SearchResponse.PostDTO> postDTOS = searchPostList(keyword, pageable);

        // 그룹 검색
        List<SearchResponse.GroupDTO> groupDTOS = searchGroupList(keyword, pageable);

        return new SearchResponse.SearchAllDTO(shelterDTOS, postDTOS, groupDTOS);
    }

    @Transactional(readOnly = true)
    public List<SearchResponse.ShelterDTO> searchShelterList(String keyword, Pageable pageable){
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);
        Page<Shelter> shelters = shelterRepository.findByNameContaining(formattedKeyword, pageable);

        List<SearchResponse.ShelterDTO> shelterDTOS = shelters.getContent().stream()
                .filter(shelter -> shelter.getAnimalCnt() > 0 && shelter.getLatitude() != null)
                .map(shelter -> new SearchResponse.ShelterDTO(shelter.getId(), shelter.getName()))
                .collect(Collectors.toList());

        return shelterDTOS;
    }

    @Transactional(readOnly = true)
    public List<SearchResponse.PostDTO> searchPostList(String keyword, Pageable pageable){
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);
        Page<Post> posts = postRepository.findByTitleContaining(formattedKeyword, pageable);

        List<SearchResponse.PostDTO> postDTOS = posts.getContent().stream()
                .map(post -> new SearchResponse.PostDTO(
                        post.getId(),
                        post.getTitle(),
                        post.getContent(),
                        post.getCreatedDate(),
                        post.getPostImages().get(0).getImageURL()))
                .collect(Collectors.toList());

        return postDTOS;
    }

    @Transactional(readOnly = true)
    public List<SearchResponse.GroupDTO> searchGroupList(String keyword, Pageable pageable){
        String formattedKeyword = formatKeywordForFullTextSearch(keyword);
        Page<Group> groups = groupRepository.findByNameContaining(formattedKeyword, pageable);

        List<SearchResponse.GroupDTO> groupDTOS = groups.getContent().stream()
                .map(group -> new SearchResponse.GroupDTO(
                        group.getId(),
                        group.getName(),
                        group.getDescription(),
                        group.getCategory(),
                        group.getProvince(),
                        group.getDistrict(),
                        group.getProfileURL())
                )
                .collect(Collectors.toList());

        return groupDTOS;
    }

    public void checkKeywordEmpty(String keyword) {
        if(keyword == null || keyword.trim().isEmpty()){
            throw new CustomException(ExceptionCode.SEARCH_KEYWORD_EMPTY);
        }
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
