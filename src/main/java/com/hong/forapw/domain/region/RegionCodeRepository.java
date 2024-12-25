package com.hong.forapw.domain.region;

import com.hong.forapw.domain.region.entity.RegionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegionCodeRepository extends JpaRepository<RegionCode, Long> {

}
