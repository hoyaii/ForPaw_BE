package com.hong.ForPaw.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.domain.regionCode.RegionCode;
import com.hong.ForPaw.domain.shelter.Shelter;
import com.hong.ForPaw.domain.shelter.ShelterDTO;
import com.hong.ForPaw.domain.shelter.ShelterWrapperDTO;
import com.hong.ForPaw.repository.RegionCodeRepository;
import com.hong.ForPaw.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

                ShelterWrapperDTO responseDTO = mapper.readValue(response, ShelterWrapperDTO.class);
                List<ShelterDTO> shelterDTOs = responseDTO.response().body().items().item();

                if (!shelterDTOs.isEmpty()) {
                    for (ShelterDTO shelterDTO : shelterDTOs) {
                        Shelter shelter = Shelter.builder().careRegNo(shelterDTO.careRegNo()).name(shelterDTO.careNm()).build();
                        shelterRepository.save(shelter);
                    }
                }
            } catch (JsonProcessingException e) {
                System.err.println("JSON 파싱 오류가 발생했습니다: " + e.getMessage());
                continue;
            } catch (Exception e) {
                // 기타 예외 처리
                System.err.println("기타 예외가 발생했습니다: " + e.getMessage());
                continue;
            }

            Thread.sleep(1000);
        }
    }
}
