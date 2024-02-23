package com.hong.ForPaw.domain.shelter;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Shelter {
    // 보호소 등록 번호
    @Id
    private Long careRegNo;

    @Column
    private String name;

    @Column
    private String careTel;

    @Column
    private String careAddr;

    @Builder
    public Shelter(Long careRegNo, String name, String careTel, String careAddr) {
        this.careRegNo = careRegNo;
        this.name = name;
        this.careTel = careTel;
        this.careAddr = careAddr;
    }
}