package com.hong.ForPaw.domain.RegionCode;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class RegionCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 시도 코드
    @Column
    private String uprCd;

    // 시군구 코드
    @Column
    private String orgCd;

    // 지역명
    @Column
    private String orgdownNm;
}
