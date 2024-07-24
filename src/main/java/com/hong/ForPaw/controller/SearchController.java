package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.SearchResponse;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.service.SearchService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/search/all")
    public ResponseEntity<?> searchAll(@RequestParam String keyword){
        SearchResponse.SearchAllDTO responseDTO = searchService.searchAll(keyword);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/search/shelters")
    public ResponseEntity<?> searchShelterList(@RequestParam String keyword){
        searchService.checkKeywordEmpty(keyword);
        List<SearchResponse.ShelterDTO> shelterDTOS = searchService.searchShelterList(keyword);
        SearchResponse.SearchShelterListDTO responseDTO = new SearchResponse.SearchShelterListDTO(shelterDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/search/posts")
    public ResponseEntity<?> searchPostList(@RequestParam String keyword){
        searchService.checkKeywordEmpty(keyword);
        List<SearchResponse.PostDTO> postDTOS = searchService.searchPostList(keyword);
        SearchResponse.SearchPostListDTO responseDTO = new SearchResponse.SearchPostListDTO(postDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/search/groups")
    public ResponseEntity<?> searchGroupList(@RequestParam String keyword){
        searchService.checkKeywordEmpty(keyword);
        List<SearchResponse.GroupDTO> groupDTOS = searchService.searchGroupList(keyword);
        SearchResponse.SearchGroupListDTO responseDTO = new SearchResponse.SearchGroupListDTO(groupDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}
