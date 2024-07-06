package com.hong.ForPaw.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.RegionsDTO;
import com.hong.ForPaw.domain.RegionCode;
import com.hong.ForPaw.repository.RegionCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class RegionCodeService {

    private final RegionCodeRepository regionCodeRepository;
    private final ObjectMapper mapper;

    public void updateRegionCodeData() throws IOException {
        InputStream inputStream = TypeReference.class.getResourceAsStream("/sigungu.json");
        RegionsDTO json = mapper.readValue(inputStream, RegionsDTO.class);

        json.regions().stream()
                .flatMap(region -> region.subRegions().stream()
                        .map(subRegion -> RegionCode.builder()
                                .uprCd(region.orgCd())
                                .uprName(region.orgdownNm())
                                .orgCd(subRegion.orgCd())
                                .orgName(subRegion.orgdownNm())
                                .build())
                )
                .forEach(regionCodeRepository::save);
    }
}
