package com.hong.forapw.domain.region.model;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;

import java.util.List;

public record RegionListDTO(List<RegionDTO> regions) {

    public record RegionDTO(Integer orgCd, Province orgdownNm, List<SubRegionDTO> subRegions) {
    }

    public record SubRegionDTO(Integer uprCd, Integer orgCd, District orgdownNm) {
    }
}
