package com.hong.forapw.controller;

import com.hong.forapw.controller.dto.ShelterResponse;
import com.hong.forapw.core.security.CustomUserDetails;
import com.hong.forapw.core.utils.ApiUtils;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.service.ShelterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ShelterController {

    private final ShelterService shelterService;
    private static final String SORT_BY_DATE = "createdDate";

    // 테스트 시에만 API를 열어둠
    @GetMapping("/shelters/import")
    public ResponseEntity<?> loadShelter(@AuthenticationPrincipal CustomUserDetails userDetails) {
        shelterService.updateNewShelters();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/shelters")
    public ResponseEntity<?> findShelterList() {
        ShelterResponse.FindShelterListDTO responseDTO = shelterService.findActiveShelterList();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, responseDTO));
    }

    @GetMapping("/shelters/{shelterId}/info")
    public ResponseEntity<?> findShelterInfoById(@PathVariable Long shelterId) {
        ShelterResponse.FindShelterInfoByIdDTO responseDTO = shelterService.findShelterInfoById(shelterId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/shelters/{shelterId}/animals")
    public ResponseEntity<?> findShelterAnimalsById(@PathVariable Long shelterId, @RequestParam String type,
                                                    @PageableDefault(size = 5, sort = SORT_BY_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ShelterResponse.FindShelterAnimalsByIdDTO responseDTO = shelterService.findAnimalsByShelter(shelterId, getUserIdSafely(userDetails), type, pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/shelters/addr")
    public ResponseEntity<?> findShelterListWithAddr() {
        ShelterResponse.FindShelterListWithAddr responseDTO = shelterService.findShelterListWithAddress();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    private Long getUserIdSafely(CustomUserDetails userDetails) {
        return Optional.ofNullable(userDetails)
                .map(CustomUserDetails::getUser)
                .map(User::getId)
                .orElse(null);
    }
}