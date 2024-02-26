package com.hong.ForPaw.service;

import com.hong.ForPaw.domain.regionCode.RegionCode;
import com.hong.ForPaw.repository.RegionCodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RegionCodeServiceTest {

    @Autowired
    public RegionCodeRepository regionCodeRepository;

    @Autowired
    public RegionCodeService regionCodeService;

    @Test
    void loadRegionData() {
        //given

        //when
        List<RegionCode> regionCodes = regionCodeRepository.findAll();

        // then
        assertFalse(regionCodes.isEmpty());

        RegionCode regionCode = regionCodes.get(0);
        assertNotNull(regionCode.getOrgCd());
        assertNotNull(regionCode.getUprCd());
    }
}