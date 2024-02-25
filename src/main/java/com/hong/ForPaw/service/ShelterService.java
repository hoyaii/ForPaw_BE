package com.hong.ForPaw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.domain.regionCode.RegionCode;
import com.hong.ForPaw.domain.shelter.Shelter;
import com.hong.ForPaw.domain.shelter.ShelterJsonDTO;
import com.hong.ForPaw.repository.RegionCodeRepository;
import com.hong.ForPaw.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

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

                ShelterJsonDTO json = mapper.readValue(response, ShelterJsonDTO.class);
                List<ShelterJsonDTO.itemDTO> itemDTOS = json.response().body().items().item();

                for (ShelterJsonDTO.itemDTO itemDTO : itemDTOS) {
                    Shelter shelter = Shelter.builder()
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
}
