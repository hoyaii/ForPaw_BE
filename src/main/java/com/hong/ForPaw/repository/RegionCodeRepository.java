package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.regionCode.RegionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegionCodeRepository extends JpaRepository<RegionCode, Long> {
    Optional<RegionCode> findByOrgdownNm(String name);
}
