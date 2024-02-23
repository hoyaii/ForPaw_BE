package com.hong.ForPaw.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.domain.RegionCode.RegionCode;
import com.hong.ForPaw.domain.Shelter.Shelter;
import com.hong.ForPaw.repository.RegionCodeRepository;
import com.hong.ForPaw.repository.ShelterRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelterService {

    private final ShelterRepository shelterRepository;
    private final RegionCodeRepository regionCodeRepository;

    @Value("${openAPI.service-key}")
    private String serviceKey;

    private WebClient webClient = WebClient.create();
    
    public void loadShelterData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String baseURL = "http://apis.data.go.kr/1543061/abandonmentPublicSrvc/shelter";
        List<RegionCode> regionCodeList = regionCodeRepository.findAll();

        for(RegionCode regionCode : regionCodeList){
            Integer uprCd = regionCode.getUprCd();
            Integer orgCd = regionCode.getOrgCd();
            String type = "json";

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(baseURL)
                            .queryParam("upr_cd", uprCd)
                            .queryParam("org_cd", orgCd)
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("_type", type)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Shelter shelter = mapper.readValue(response, Shelter.class);

            shelterRepository.save(shelter);
        }
    }
}
