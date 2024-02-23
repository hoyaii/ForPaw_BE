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

        for (RegionCode regionCode : regionCodeList) {
            Integer uprCd = regionCode.getUprCd();
            Integer orgCd = regionCode.getOrgCd();
            boolean success = false;
            int attempt = 0;
            while (!success && attempt < 4) {
                try {
                    String url = baseUrl + "?serviceKey=" + URLEncoder.encode(serviceKey, "UTF-8")
                            + "&upr_cd=" + uprCd + "&org_cd=" + orgCd + "&_type=json";
                    System.out.println("Requesting URL: " + url);

                    ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
                    String response = responseEntity.getBody();
                    System.out.println("Response: " + response);

                    ShelterJsonDTO json = mapper.readValue(response, ShelterJsonDTO.class);
                    List<ShelterJsonDTO.itemDTO> itemDTOS = json.response().body().items().item();

                    if (!itemDTOS.isEmpty()) {
                        for (ShelterJsonDTO.itemDTO itemDTO : itemDTOS) {
                            Shelter shelter = Shelter.builder().careRegNo(itemDTO.careRegNo()).name(itemDTO.careNm()).build();
                            shelterRepository.save(shelter);
                        }
                    }
                    success = true; // 성공했으므로 반복 중지
                } catch (Exception e) {
                    System.err.println("JSON 파싱 오류가 발생했습니다. 재시도 중...: ");
                    attempt++; // 시도 횟수 증가
                    Thread.sleep(2000); // 2초 대기
                }
            }
            if (!success) {
                System.err.println("3번의 시도에도 성공하지 못했습니다. 다음 지역으로 넘어갑니다.");
            }
        }
    }
}
