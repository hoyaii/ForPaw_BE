package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.ShelterResponse;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.service.ShelterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
