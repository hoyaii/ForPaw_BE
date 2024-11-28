package com.hong.forapw.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "shelter_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Shelter {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regionCode_id")
    private RegionCode regionCode;

    // 보호소 등록 번호
    @Id
    private Long id;

    @Column
    private String name;

    @Column
    private String careTel;

    @Column
    private String careAddr;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private Long animalCnt = 0L;

    @Column
    private boolean isDuplicate;

    @Builder
    public Shelter(Long id, RegionCode regionCode, String name, String careTel, String careAddr) {
        this.id = id;
        this.regionCode = regionCode;
        this.name = name;
        this.careTel = careTel;
        this.careAddr = careAddr;
        this.isDuplicate = false;
    }

    public void updateAnimalCnt(Long animalCnt) {
        this.animalCnt = animalCnt;
    }

    public void updateIsDuplicate(boolean isDuplicate) {
        this.isDuplicate = isDuplicate;
    }
}