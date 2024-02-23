package com.hong.ForPaw.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.domain.regionCode.RegionCode;
import com.hong.ForPaw.domain.regionCode.RegionsJsonDTO;
import com.hong.ForPaw.repository.RegionCodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class RegionCodeService {

    private final RegionCodeRepository regionCodeRepository;

    public void loadRegionData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = TypeReference.class.getResourceAsStream("/sigungu.json");
        RegionsJsonDTO json = mapper.readValue(inputStream, RegionsJsonDTO.class);

        json.regions().forEach(region -> region.subRegions().forEach(subRegion -> {
            RegionCode regionCode = new RegionCode();
            regionCode.setUprCd(region.orgCd());
            regionCode.setOrgCd(subRegion.orgCd());
            regionCode.setOrgdownNm(subRegion.orgdownNm());
            regionCodeRepository.save(regionCode);
        }));
    }
}
