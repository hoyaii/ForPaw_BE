package com.hong.forapw.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.forapw.controller.dto.RegionListDTO;
import com.hong.forapw.domain.RegionCode;
import com.hong.forapw.repository.RegionCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionCodeService {

    private final RegionCodeRepository regionCodeRepository;
    private final ObjectMapper mapper;

    @Value("${region.code.file.path}")
    private String regionCodeFilePath;

    public void fetchRegionCode() {
        try {
            Optional.ofNullable(loadRegionDtoFromFile())
                    .map(this::convertToRegionCodes)
                    .ifPresent(regionCodeRepository::saveAll);
        } catch (IOException e) {
            log.error("지역 코드 패치 실패: {}", regionCodeFilePath, e);
        }
    }

    private RegionListDTO loadRegionDtoFromFile() throws IOException {
        try (InputStream inputStream = TypeReference.class.getResourceAsStream(regionCodeFilePath)) {
            if (inputStream == null) {
                return null;
            }
            return mapper.readValue(inputStream, RegionListDTO.class);
        }
    }

    private List<RegionCode> convertToRegionCodes(RegionListDTO regionListDTO) {
        return regionListDTO.regions().stream()
                .flatMap(region -> region.subRegions().stream()
                        .map(subRegion -> RegionCode.builder()
                                .uprCd(region.orgCd())
                                .uprName(region.orgdownNm())
                                .orgCd(subRegion.orgCd())
                                .orgName(subRegion.orgdownNm())
                                .build())
                )
                .collect(Collectors.toList());
    }
}
