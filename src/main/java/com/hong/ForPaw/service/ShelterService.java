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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelterService {

    private final ShelterRepository shelterRepository;
    private final RegionCodeRepository regionCodeRepository;
    private final WebClient.Builder webClientBuilder; // WebClient.Builder 주입

    @Value("${openAPI.service-key}")
    private String serviceKey;

    @Value("${openAPI.careURL}")
    private String baseURL;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.baseUrl(baseURL).build(); // 기본 URL 설정
    }

    public void loadShelterData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<RegionCode> regionCodeList = regionCodeRepository.findAll();

        for(RegionCode regionCode : regionCodeList){
            Integer uprCd = regionCode.getUprCd();
            Integer orgCd = regionCode.getOrgCd();

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("upr_cd", uprCd)
                            .queryParam("org_cd", orgCd)
                            .queryParam("serviceKey", URLEncoder.encode(serviceKey, StandardCharsets.UTF_8))
                            .queryParam("_type", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Shelter shelter = mapper.readValue(response, Shelter.class);

            shelterRepository.save(shelter);
        }
    }
}
