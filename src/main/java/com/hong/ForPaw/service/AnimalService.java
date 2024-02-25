package com.hong.ForPaw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.domain.shelter.Shelter;
import com.hong.ForPaw.repository.AnimalRepository;
import com.hong.ForPaw.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final ShelterRepository shelterRepository;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${openAPI.careURL}")
    private String baseUrl;

    @Transactional
    public void loadAnimalDate() throws URISyntaxException {
        // shelter 돌면서 => animal 정보 쭉 떙겨온다 => shelter를 패치하고 => animal 등록
        ObjectMapper mapper = new ObjectMapper();
        RestTemplate restTemplate = new RestTemplate();

        List<Shelter> shelters = shelterRepository.findAll();

        for(Shelter shelter : shelters){
            Long careRegNo = shelter.getCareRegNo();

            String url = baseUrl + "?serviceKey=" + serviceKey + "&care_reg_no=" + careRegNo +  "&_type=json";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "*/*;q=0.9"); // HTTP_ERROR 방지
            HttpEntity<?> entity = new HttpEntity<>(null, headers);

            URI uri = new URI(url);

            ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            String response = responseEntity.getBody();

            // ShelterJsonDTO json = mapper.readValue(response, ShelterJsonDTO.class);
        }
    }
}
