package com.hong.forapw.controller;

import com.hong.forapw.controller.dto.PostRequest;
import com.hong.forapw.controller.dto.PostResponse;
import com.hong.forapw.core.security.CustomUserDetails;
import com.hong.forapw.core.utils.ApiUtils;
import com.hong.forapw.domain.post.PostType;
import com.hong.forapw.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class PostController {

    private final PostService postService;
    private static final String SORT_BY_ID = "id";

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(@RequestBody @Valid PostRequest.CreatePostDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostResponse.CreatePostDTO responseDTO = postService.createPost(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/posts/{postId}/qna")
    public ResponseEntity<?> createAnswer(@RequestBody @Valid PostRequest.CreateAnswerDTO requestDTO, @PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostResponse.CreateAnswerDTO responseDTO = postService.createAnswer(requestDTO, postId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/posts/adoption")
    public ResponseEntity<?> findAdoptionPostList(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable) {
        PostResponse.FindPostListDTO responseDTO = postService.findPostsByType(pageable, PostType.ADOPTION);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/posts/fostering")
    public ResponseEntity<?> findFosteringPostList(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable) {
        PostResponse.FindPostListDTO responseDTO = postService.findPostsByType(pageable, PostType.FOSTERING);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/posts/question")
    public ResponseEntity<?> findQuestionList(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable) {
        PostResponse.FindQnaListDTO responseDTO = postService.findQuestions(pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/posts/popular")
    public ResponseEntity<?> findPopularPostList(@RequestParam PostType type,
                                                 @PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable) {
        PostResponse.FindPostListDTO responseDTO = postService.findPopularPostsByType(pageable, type);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/posts/myPost")
    public ResponseEntity<?> findMyPosts(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostResponse.FindMyPostListDTO responseDTO = postService.findMyPosts(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/posts/myQuestion")
    public ResponseEntity<?> findMyQuestions(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostResponse.FindQnaListDTO responseDTO = postService.findMyQuestions(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/posts/myAnswer")
    public ResponseEntity<?> findQuestionsAnsweredByMe(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostResponse.FindQnaListDTO responseDTO = postService.findQuestionsAnsweredByMe(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/posts/myComment")
    public ResponseEntity<?> findMyCommentList(@PageableDefault(size = 5, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostResponse.FindMyCommentListDTO responseDTO = postService.findMyComments(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<?> findPostById(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostResponse.FindPostByIdDTO responseDTO = postService.findPostById(postId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/posts/{postId}/qna")
    public ResponseEntity<?> findQnaById(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostResponse.FindQnaByIdDTO responseDTO = postService.findQnaById(postId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/answers/{answerId}")
    public ResponseEntity<?> findAnswerById(@PathVariable Long answerId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostResponse.FindAnswerByIdDTO responseDTO = postService.findAnswerById(answerId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/posts/{postId}")
    public ResponseEntity<?> updatePost(@RequestBody @Valid PostRequest.UpdatePostDTO requestDTO, @PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.updatePost(requestDTO, userDetails.getUser(), postId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deletePost(postId, userDetails.getUser());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<?> deleteAnswer(@PathVariable Long answerId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deleteAnswer(answerId, userDetails.getUser());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.likePost(postId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> createComment(@RequestBody @Valid PostRequest.CreateCommentDTO requestDTO, @PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostResponse.CreateCommentDTO responseDTO = postService.createComment(requestDTO, userDetails.getUser().getId(), postId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/reply")
    public ResponseEntity<?> createReply(@RequestBody @Valid PostRequest.CreateCommentDTO requestDTO, @PathVariable Long postId, @PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostResponse.CreateCommentDTO responseDTO = postService.createReply(requestDTO, postId, userDetails.getUser().getId(), commentId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<?> updateComment(@RequestBody @Valid PostRequest.UpdateCommentDTO requestDTO, @PathVariable Long postId, @PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.updateComment(requestDTO, postId, commentId, userDetails.getUser());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long postId, @PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deleteComment(postId, commentId, userDetails.getUser());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/like")
    public ResponseEntity<?> likeComment(@PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.likeComment(commentId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/reports")
    public ResponseEntity<?> submitReport(@RequestBody @Valid PostRequest.SubmitReport requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.submitReport(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}