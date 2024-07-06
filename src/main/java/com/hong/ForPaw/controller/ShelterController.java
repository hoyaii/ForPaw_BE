package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.ShelterResponse;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.service.ShelterService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/shelters/import")
    public ResponseEntity<?> loadShelter(@AuthenticationPrincipal CustomUserDetails userDetails) {
        shelterService.updateShelterData();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/shelters")
    public ResponseEntity<?> findShelterList(){
        ShelterResponse.FindShelterListDTO responseDTO = shelterService.findShelterList();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, responseDTO));
    }

    @GetMapping("/shelters/{shelterId}/info")
    public ResponseEntity<?> findShelterInfoById(@PathVariable Long shelterId){
        ShelterResponse.FindShelterInfoByIdDTO responseDTO = shelterService.findShelterInfoById(shelterId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/shelters/{shelterId}/animals")
    public ResponseEntity<?> findShelterAnimalsById(@PathVariable Long shelterId, @RequestParam("page") Integer page, @RequestParam(value = "sort", defaultValue = "noticeSdt") String sort, @AuthenticationPrincipal CustomUserDetails userDetails){
        Long userId = Optional.ofNullable(userDetails)
                .map(CustomUserDetails::getUser)
                .map(User::getId)
                .orElse(null);

        ShelterResponse.FindShelterAnimalsByIdDTO responseDTO = shelterService.findShelterAnimalsById(shelterId, userId, page, sort);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}