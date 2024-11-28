package com.hong.forapw.repository;

import com.hong.forapw.domain.RegionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegionCodeRepository extends JpaRepository<RegionCode, Long> {

}
