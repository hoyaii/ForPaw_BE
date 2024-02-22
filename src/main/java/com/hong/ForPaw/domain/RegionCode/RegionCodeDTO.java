package com.hong.ForPaw.domain.RegionCode;

import java.util.List;

public record RegionCodeDTO(Integer orgCd, String orgdownNm, List<SubRegionCodeDTO> subRegions){ }
