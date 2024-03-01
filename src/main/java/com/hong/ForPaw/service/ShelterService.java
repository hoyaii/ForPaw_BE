package com.hong.ForPaw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.AnimalResponse;
import com.hong.ForPaw.controller.DTO.ShelterResponse;
import com.hong.ForPaw.domain.RegionCode;
import com.hong.ForPaw.controller.DTO.ShelterDTO;
import com.hong.ForPaw.domain.Shelter;
import com.hong.ForPaw.repository.RegionCodeRepository;
import com.hong.ForPaw.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShelterService {

    private final ShelterRepository shelterRepository;
    private final RegionCodeRepository regionCodeRepository;

    @Value("${openAPI.service-key2}")
    private String serviceKey;

    @Value("${openAPI.careURL}")
    private String baseUrl;

    @Transactional
    public void loadShelterData() {
        ObjectMapper mapper = new ObjectMapper();
        RestTemplate restTemplate = new RestTemplate();

        List<RegionCode> regionCodeList = regionCodeRepository.findAll();

        for (RegionCode regionCode : regionCodeList) {
            Integer uprCd = regionCode.getUprCd();
            Integer orgCd = regionCode.getOrgCd();

            try {
                String url = baseUrl + "?serviceKey=" + serviceKey + "&upr_cd=" + uprCd + "&org_cd=" + orgCd + "&_type=json";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept", "*/*;q=0.9"); // HTTP_ERROR 방지
                HttpEntity<?> entity = new HttpEntity<>(null, headers);

                URI uri = new URI(url);

                ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
                String response = responseEntity.getBody();

                ShelterDTO json = mapper.readValue(response, ShelterDTO.class);
                List<ShelterDTO.itemDTO> itemDTOS = json.response().body().items().item();

                for (ShelterDTO.itemDTO itemDTO : itemDTOS) {
                    com.hong.ForPaw.domain.Shelter shelter = com.hong.ForPaw.domain.Shelter.builder()
                            .regionCode(regionCode)
                            .careRegNo(itemDTO.careRegNo())
                            .name(itemDTO.careNm()).build();

                    shelterRepository.save(shelter);
                }

            } catch (Exception e) {
                System.err.println("JSON 파싱 오류가 발생했습니다. 재시도 중...: ");
                System.out.println(e);
            }
        }
    }

    @Transactional
    public ShelterResponse.FindAllSheltersDTO findAllShelters(Pageable pageable){

        Page<Shelter> shelterPage = shelterRepository.findAll(pageable);

        List<ShelterResponse.ShelterDTO> shelterDTOS = shelterPage.getContent().stream()
                .map(shelter -> new ShelterResponse.ShelterDTO(shelter.getCareRegNo(), shelter.getName(),
                        shelter.getCareAddr(), shelter.getCareTel()))
                .collect(Collectors.toList());

        return new ShelterResponse.FindAllSheltersDTO(shelterDTOS);
    }
}
