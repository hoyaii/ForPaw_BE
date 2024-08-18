package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.District;
import com.hong.ForPaw.domain.Province;

import java.util.List;

public record RegionListDTO(List<RegionDTO> regions) {

    public record RegionDTO(Integer orgCd, Province orgdownNm, List<SubRegionDTO> subRegions) { }

    public record SubRegionDTO(Integer uprCd, Integer orgCd, District orgdownNm) { }
}
