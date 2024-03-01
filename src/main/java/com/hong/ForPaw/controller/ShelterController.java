package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.ShelterResponse;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.service.ShelterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ShelterController {

    private final ShelterService shelterService;

    @GetMapping("/shelters/import")
    public ResponseEntity<?> loadShelter() {

        shelterService.loadShelterData();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/shelters")
    public ResponseEntity<?> findAllShelters(Pageable pageable){

        ShelterResponse.FindAllSheltersDTO responseDTO = shelterService.findAllShelters(pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, responseDTO));
    }

    @DeleteMapping("/shelters")
    public ResponseEntity<?> deleteZeroShelter(@AuthenticationPrincipal CustomUserDetails userDetails){

        shelterService.deleteZeroShelter(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}
