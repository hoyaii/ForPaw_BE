package com.hong.ForPaw.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hong.ForPaw.controller.DTO.AnimalResponse;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.service.AnimalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URISyntaxException;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class AnimalController {

    private final AnimalService animalService;

    @GetMapping("/animals/import")
    public ResponseEntity<?> loadAnimals() throws URISyntaxException, JsonProcessingException {
        animalService.loadAnimalDate();

        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/animals")
    public ResponseEntity<?> findAllAnimals(Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        AnimalResponse.FindAllAnimalsDTO responseDTO = animalService.findAllAnimals(pageable, userDetails.getUser());

        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.CREATED, responseDTO));
    }
}