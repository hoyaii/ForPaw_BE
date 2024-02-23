package com.hong.ForPaw.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.domain.RegionCode.RegionCode;
import com.hong.ForPaw.domain.RegionCode.RegionCodeDTO;
import com.hong.ForPaw.domain.RegionCode.RegionsWrapperDTO;
import com.hong.ForPaw.repository.RegionCodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionCodeService {

    private final RegionCodeRepository regionCodeRepository;

    @PostConstruct
    public void loadRegionData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = TypeReference.class.getResourceAsStream("/sigungu.json");
        RegionsWrapperDTO regionsWrapper = mapper.readValue(inputStream, RegionsWrapperDTO.class);

        regionsWrapper.regions().forEach(regionDto -> regionDto.subRegions().forEach(subRegionCodeDTO -> {
            RegionCode regionCode = new RegionCode();
            regionCode.setUprCd(regionDto.orgCd());
            regionCode.setOrgCd(subRegionCodeDTO.orgCd());
            regionCode.setOrgdownNm(subRegionCodeDTO.orgdownNm());
            regionCodeRepository.save(regionCode);
        }));
    }
}
