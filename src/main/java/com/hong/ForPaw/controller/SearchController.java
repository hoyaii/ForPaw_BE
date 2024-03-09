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
    public ResponseEntity<?> searchShelters(@RequestParam String keyword, @RequestParam Integer page, @RequestParam Integer size){

        SearchResponse.SearchSheltersDTO responseDTO = searchService.searchShelters(keyword, page, size);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/search/posts")
    public ResponseEntity<?> searchPosts(@RequestParam String keyword, @RequestParam Integer page, @RequestParam Integer size){

        SearchResponse.SearchPostsDTO responseDTO = searchService.searchPosts(keyword, page, size);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/search/groups")
    public ResponseEntity<?> searchGroups(@RequestParam String keyword, @RequestParam Integer page, @RequestParam Integer size){

        SearchResponse.SearchGroupsDTO responseDTO = searchService.searchGroups(keyword, page, size);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}
