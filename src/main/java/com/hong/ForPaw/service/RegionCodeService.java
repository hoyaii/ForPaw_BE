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

    @PostConstruct
    public void loadRegionData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = TypeReference.class.getResourceAsStream("/sigungu.json");
        RegionsJsonDTO regionsWrapper = mapper.readValue(inputStream, RegionsJsonDTO.class);

        regionsWrapper.regions().forEach(regionDto -> regionDto.subRegions().forEach(subRegionDto -> {
            RegionCode regionCode = new RegionCode();
            regionCode.setUprCd(regionDto.orgCd());
            regionCode.setOrgCd(subRegionDto.orgCd());
            regionCode.setOrgdownNm(subRegionDto.orgdownNm());
            regionCodeRepository.save(regionCode);
        }));
    }
}
