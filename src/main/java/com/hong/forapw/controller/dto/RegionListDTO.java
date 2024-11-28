package com.hong.forapw.controller.dto;

import com.hong.forapw.domain.District;
import com.hong.forapw.domain.Province;

import java.util.List;

public record RegionListDTO(List<RegionDTO> regions) {

    public record RegionDTO(Integer orgCd, Province orgdownNm, List<SubRegionDTO> subRegions) {
    }

    public record SubRegionDTO(Integer uprCd, Integer orgCd, District orgdownNm) {
    }
}
