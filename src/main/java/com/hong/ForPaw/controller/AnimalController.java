package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.AnimalRequest;
import com.hong.ForPaw.controller.DTO.AnimalResponse;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.service.AnimalService;
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

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class AnimalController {

    private final AnimalService animalService;

    @GetMapping("/animals/import")
    public ResponseEntity<?> loadAnimals() {
        animalService.updateAnimalData();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/animals/recommend")
    public ResponseEntity<?> findRecommendedAnimalList(@AuthenticationPrincipal CustomUserDetails userDetails){
        Long userId = getUserIdSafely(userDetails);
        AnimalResponse.FindRecommendedAnimalList responseDTO = animalService.findRecommendedAnimalList(userId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/animals")
    public ResponseEntity<?> findAnimalList(@RequestParam Integer page, @RequestParam String sort, @AuthenticationPrincipal CustomUserDetails userDetails){
        Long userId = getUserIdSafely(userDetails);
        AnimalResponse.FindAnimalListDTO responseDTO = animalService.findAnimalList(page, sort, userId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/animals/like")
    public ResponseEntity<?> findLikeAnimalList(@PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        AnimalResponse.FindLikeAnimalListDTO responseDTO = animalService.findLikeAnimalList(pageable, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/animals/{animalId}")
    public ResponseEntity<?> findAnimalById(@PathVariable Long animalId, @AuthenticationPrincipal CustomUserDetails userDetails){
        Long userId = getUserIdSafely(userDetails);
        AnimalResponse.FindAnimalByIdDTO responseDTO = animalService.findAnimalById(animalId, userId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/animals/{animalId}/like")
    public ResponseEntity<?> likeAnimal(@PathVariable Long animalId, @AuthenticationPrincipal CustomUserDetails userDetails){
        animalService.likeAnimal(userDetails.getUser().getId(), animalId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @PostMapping("/animals/{animalId}/apply")
    public ResponseEntity<?> applyAdoption(@RequestBody @Valid AnimalRequest.ApplyAdoptionDTO requestDTO, @PathVariable Long animalId, @AuthenticationPrincipal CustomUserDetails userDetails){
        AnimalResponse.CreateApplyDTO responseDTO = animalService.applyAdoption(requestDTO, userDetails.getUser().getId(), animalId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/applies")
    public ResponseEntity<?> findApplyList(@AuthenticationPrincipal CustomUserDetails userDetails){
        AnimalResponse.FindApplyListDTO responseDTO = animalService.findApplyList(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/applies/{applyId}")
    public ResponseEntity<?> updateApply(@RequestBody @Valid AnimalRequest.UpdateApplyDTO requestDTO, @PathVariable Long applyId, @AuthenticationPrincipal CustomUserDetails userDetails){
        animalService.updateApply(requestDTO, applyId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    // 권한 처리가 필요함
    @DeleteMapping ("/applies/{applyId}")
    public ResponseEntity<?> deleteApply(@PathVariable Long applyId, @AuthenticationPrincipal CustomUserDetails userDetails){
        animalService.deleteApply(applyId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    private Long getUserIdSafely(CustomUserDetails userDetails) {
        return Optional.ofNullable(userDetails)
                .map(CustomUserDetails::getUser)
                .map(User::getId)
                .orElse(null);
    }
}