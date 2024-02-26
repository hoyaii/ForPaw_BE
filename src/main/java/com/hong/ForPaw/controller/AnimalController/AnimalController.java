package com.hong.ForPaw.controller.AnimalController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hong.ForPaw.service.AnimalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URISyntaxException;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class AnimalController {

    private final AnimalService animalService;

    @GetMapping("/animals")
    public String loadAnimals() throws URISyntaxException, JsonProcessingException {
        animalService.loadAnimalDate();

        return "ho";
    }

    @GetMapping("/animals")
    public ResponseEntity<?> findAllAnimals(Pageable pageable){
        // JPA는 animal List를 찾아서 준다}

        return
    }
}
