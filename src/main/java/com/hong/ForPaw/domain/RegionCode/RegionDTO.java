package com.hong.ForPaw.domain.RegionCode;

import java.util.List;

public record RegionDTO(String orgCd, String orgdownNm, List<SubRegionDto> subRegions){ }
