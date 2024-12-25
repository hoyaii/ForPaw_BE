package com.hong.forapw.domain.search;

import com.hong.forapw.common.utils.ApiUtils;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;
    private static final String SORT_BY_ID = "id";

    @GetMapping("/search/all")
    public ResponseEntity<?> searchAll(@RequestParam @NotEmpty String keyword,
                                       @PageableDefault(size = 3, sort = SORT_BY_ID) Pageable pageable) {
        SearchResponse.SearchAllDTO responseDTO = searchService.searchAll(keyword, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/search/shelters")
    public ResponseEntity<?> searchShelterList(@RequestParam @NotEmpty String keyword,
                                               @PageableDefault(size = 3, sort = SORT_BY_ID) Pageable pageable) {
        List<SearchResponse.ShelterDTO> shelterDTOS = searchService.searchShelterList(keyword, pageable);
        SearchResponse.SearchShelterListDTO responseDTO = new SearchResponse.SearchShelterListDTO(shelterDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/search/posts")
    public ResponseEntity<?> searchPostList(@RequestParam @NotEmpty String keyword,
                                            @PageableDefault(size = 3, sort = SORT_BY_ID) Pageable pageable) {
        List<SearchResponse.PostDTO> postDTOS = searchService.searchPostList(keyword, pageable);
        SearchResponse.SearchPostListDTO responseDTO = new SearchResponse.SearchPostListDTO(postDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/search/groups")
    public ResponseEntity<?> searchGroupList(@RequestParam @NotEmpty String keyword,
                                             @PageableDefault(size = 3, sort = SORT_BY_ID) Pageable pageable) {
        List<SearchResponse.GroupDTO> groupDTOS = searchService.searchGroupList(keyword, pageable);
        SearchResponse.SearchGroupListDTO responseDTO = new SearchResponse.SearchGroupListDTO(groupDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}
