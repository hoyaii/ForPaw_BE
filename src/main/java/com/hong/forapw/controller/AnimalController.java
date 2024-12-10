package com.hong.forapw.controller;

import com.hong.forapw.controller.dto.AnimalResponse;
import com.hong.forapw.core.security.CustomUserDetails;
import com.hong.forapw.core.utils.ApiUtils;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.service.AnimalService;
import com.hong.forapw.service.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class AnimalController {

    private final AnimalService animalService;
    private final LikeService likeService;
    private static final String SORT_BY_DATE = "createdDate";

    // 테스트시에만 열어둠
    @GetMapping("/animals/import")
    public ResponseEntity<?> loadAnimals() {
        animalService.updateNewAnimals();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/animals/recommend")
    public ResponseEntity<?> findRecommendedAnimalList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        AnimalResponse.FindRecommendedAnimalList responseDTO = animalService.findRecommendedAnimalList(getUserIdSafely(userDetails));
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/animals")
    public ResponseEntity<?> findAnimalList(@RequestParam String type,
                                            @PageableDefault(size = 5, sort = SORT_BY_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        AnimalResponse.FindAnimalListDTO responseDTO = animalService.findAnimalList(type, getUserIdSafely(userDetails), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/animals/like")
    public ResponseEntity<?> findLikeAnimalList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        AnimalResponse.FindLikeAnimalListDTO responseDTO = animalService.findLikeAnimalList(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/animals/{animalId}")
    public ResponseEntity<?> findAnimalById(@PathVariable Long animalId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        AnimalResponse.FindAnimalByIdDTO responseDTO = animalService.findAnimalById(animalId, getUserIdSafely(userDetails));
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/animals/{animalId}/like")
    public ResponseEntity<?> likeAnimal(@PathVariable Long animalId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        likeService.likeAnimal(animalId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    private Long getUserIdSafely(CustomUserDetails userDetails) {
        return Optional.ofNullable(userDetails)
                .map(CustomUserDetails::getUser)
                .map(User::getId)
                .orElse(null);
    }
}