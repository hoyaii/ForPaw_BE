package com.hong.ForPaw.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.domain.RegionCode.RegionCode;
import com.hong.ForPaw.domain.Shelter.Shelter;
import com.hong.ForPaw.domain.Shelter.ShelterDTO;
import com.hong.ForPaw.domain.Shelter.ShelterWrapperDTO;
import com.hong.ForPaw.repository.RegionCodeRepository;
import com.hong.ForPaw.repository.ShelterRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShelterService {

    private final ShelterRepository shelterRepository;
    private final RegionCodeRepository regionCodeRepository;
    // private final WebClient.Builder webClientBuilder;

    @Value("${openAPI.service-key}")
    private String serviceKey;

    // private WebClient webClient;

    @Transactional
    public void loadShelterData() throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        List<RegionCode> regionCodeList = regionCodeRepository.findAll();
        RestTemplate restTemplate = new RestTemplate();

        for(RegionCode regionCode : regionCodeList) {
            Integer uprCd = regionCode.getUprCd();
            Integer orgCd = regionCode.getOrgCd();

            String url = "http://apis.data.go.kr/1543061/abandonmentPublicSrvc/shelter";
            url += "?serviceKey=" + URLEncoder.encode(serviceKey, "UTF-8");
            url += "&upr_cd=" + uprCd;
            url += "&org_cd=" + orgCd;
            url += "&_type=json";

            System.out.println("Requesting URL: " + url);

            ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
            String response = responseEntity.getBody();

            System.out.println("Response: " + response);

            ShelterWrapperDTO responseDTO = mapper.readValue(response, ShelterWrapperDTO.class);
            List<ShelterDTO> shelterDTOs = responseDTO.response().body().items().item();

            if(!shelterDTOs.isEmpty()) {
                for (ShelterDTO shelterDTO : shelterDTOs) {
                    Shelter shelter = Shelter.builder().careRegNo(shelterDTO.careRegNo()).name(shelterDTO.careNm()).build();
                    shelterRepository.save(shelter);
                }
            }

            // 요청 사이에 지연
            Thread.sleep(1500);
        }
    }

/*
    public void loadShelterData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<RegionCode> regionCodeList = regionCodeRepository.findAll();

        for(RegionCode regionCode : regionCodeList){
            Integer uprCd = regionCode.getUprCd();
            Integer orgCd = regionCode.getOrgCd();

            String url = "http://apis.data.go.kr/1543061/abandonmentPublicSrvc/shelter";
            url += ("?&serviceKey=" + serviceKey);
            url += ("&upr_cd=" + uprCd);
            url += ("&org_cd=" + orgCd);
            url += ("&_type=" + "json");

            System.out.println("----------"+ url + "---------------------------------");

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("----------"+ response + "---------------------------------");

            ShelterWrapperDTO responseDTO = mapper.readValue(response, ShelterWrapperDTO.class);
            List<ShelterDTO> shelterDTOs = responseDTO.response().body().items().item();

           if(shelterDTOs.) {
                for (ShelterDTO shelterDTO : shelterDTOs) {
                    Shelter shelter = Shelter.builder().careRegNo(shelterDTO.careRegNo()).name(shelterDTO.careNm()).build();
                    shelterRepository.save(shelter);
                }
            }
        }
    }*/
}
