package com.hong.ForPaw.domain.regionCode;

import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RegionCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 시도 코드
    @Column
    private Integer uprCd;

    // 시군구 코드
    @Column
    private Integer orgCd;

    // 지역명
    @Column
    private String orgdownNm;

    @Builder
    public RegionCode(Integer uprCd, Integer orgCd, String orgdownNm) {
        this.uprCd = uprCd;
        this.orgCd = orgCd;
        this.orgdownNm = orgdownNm;
    }
}
