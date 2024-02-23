package com.hong.ForPaw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.domain.regionCode.RegionCode;
import com.hong.ForPaw.domain.shelter.Shelter;
import com.hong.ForPaw.domain.shelter.ShelterJsonDTO;
import com.hong.ForPaw.repository.RegionCodeRepository;
import com.hong.ForPaw.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShelterService {

    private final ShelterRepository shelterRepository;
    private final RegionCodeRepository regionCodeRepository;

    @Value("${openAPI.service-key}")
    private String serviceKey;

    @Value("${openAPI.careURL}")
    private String baseUrl;

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

            try {
                ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
                String response = responseEntity.getBody();
                System.out.println("Response: " + response);

                ShelterJsonDTO responseDTO = mapper.readValue(response, ShelterJsonDTO.class);
                List<ShelterJsonDTO.itemDTO> itemDTOS = responseDTO.response().body().items().item();

                if (!itemDTOS.isEmpty()) {
                    for (ShelterJsonDTO.itemDTO itemDTO : itemDTOS) {
                        Shelter shelter = Shelter.builder().careRegNo(itemDTO.careRegNo()).name(itemDTO.careNm()).build();
                        shelterRepository.save(shelter);
                    }
                }
            } catch (Exception e) {
                System.err.println("JSON 파싱 오류가 발생했습니다: " + e.getMessage());
                continue;
            }

            Thread.sleep(1000);
        }
    }
}
