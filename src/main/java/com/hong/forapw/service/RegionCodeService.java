package com.hong.forapw.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.forapw.controller.dto.RegionListDTO;
import com.hong.forapw.domain.RegionCode;
import com.hong.forapw.repository.RegionCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class RegionCodeService {

    private final RegionCodeRepository regionCodeRepository;
    private final ObjectMapper mapper;
    private static final String REGION_CODE_FILE_PATH = "/sigungu.json";

    public void updateRegionCodeData() throws IOException {
        InputStream inputStream = TypeReference.class.getResourceAsStream(REGION_CODE_FILE_PATH);
        RegionListDTO regionListDTO = mapper.readValue(inputStream, RegionListDTO.class);

        regionListDTO.regions().stream()
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
