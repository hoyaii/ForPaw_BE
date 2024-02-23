package com.hong.ForPaw.domain.regionCode;

import java.util.List;

public record RegionCodeDTO(Integer orgCd, String orgdownNm, List<SubRegionCodeDTO> subRegions){ }
