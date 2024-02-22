package com.hong.ForPaw.service;

import com.hong.ForPaw.domain.RegionCode.RegionCode;
import com.hong.ForPaw.repository.RegionCodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RegionCodeServiceTest {

    @Autowired
    RegionCodeRepository regionCodeRepository;

    @Autowired
    RegionCodeService regionCodeService;

    @Test
    void loadRegionData() {
        //given

        //when
        RegionCode regionCode = regionCodeRepository.getReferenceById(1L);

        //then
        System.out.println(regionCode.getOrgCd());
    }
}