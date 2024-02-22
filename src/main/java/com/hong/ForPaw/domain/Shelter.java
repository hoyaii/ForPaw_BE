package com.hong.ForPaw.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shelter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private String careTel;

    @Column
    private String careAddr;

    @Builder
    public Shelter(Long id, String name, String careTel, String careAddr) {
        this.name = name;
        this.careTel = careTel;
        this.careAddr = careAddr;
    }
}
