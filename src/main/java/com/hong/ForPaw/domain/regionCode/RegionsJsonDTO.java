package com.hong.ForPaw.domain.regionCode;

import java.util.List;

public record RegionsJsonDTO(List<RegionDTO> regions) {

    public record RegionDTO(Integer orgCd, String orgdownNm, List<SubRegionDTO> subRegions) { }

    public record SubRegionDTO(Integer uprCd, Integer orgCd, String orgdownNm) { }
}