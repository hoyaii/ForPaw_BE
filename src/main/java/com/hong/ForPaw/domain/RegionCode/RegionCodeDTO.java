package com.hong.ForPaw.domain.RegionCode;

import java.util.List;

public record RegionCodeDTO(String orgCd, String orgdownNm, List<SubRegionCodeDTO> subRegions){ }
