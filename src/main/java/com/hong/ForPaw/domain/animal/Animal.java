package com.hong.ForPaw.domain.animal;

import com.hong.ForPaw.domain.shelter.Shelter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Animal {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gender_id")
    private Shelter shelter;

    @Id
    private Long desertionNo;

    @Column
    private LocalDate happenDt;

    @Column
    private String happenPlace;

    @Column
    private String kind;

    @Column
    private String color;

    @Column
    private String age;

    @Column
    private String weight;

    // 공고 시작일
    @Column
    private LocalDate noticeSdt;

    @Column
    private LocalDate noticeEdt;

    @Column
    private String profileURL;

    // 보호중 여부
    @Column
    private String processState;

    @Column
    private String gender;

    // 중성화 여부
    @Column
    private String neuter;

    // 특징
    @Column
    private String specialMark;

    @Column
    private Integer likeNum = 0;

    @Column
    private Integer inquiryNum = 0;


    @Builder
    public Animal(Shelter shelter, Long desertionNo, LocalDate happenDt, String happenPlace, String kind, String color, String age, String weight, LocalDate noticeSdt, LocalDate noticeEdt, String profileURL, String processState, String gender, String neuter, String specialMark) {
        this.shelter = shelter;
        this.desertionNo = desertionNo;
        this.happenDt = happenDt;
        this.happenPlace = happenPlace;
        this.kind = kind;
        this.color = color;
        this.age = age;
        this.weight = weight;
        this.noticeSdt = noticeSdt;
        this.noticeEdt = noticeEdt;
        this.profileURL = profileURL;
        this.processState = processState;
        this.gender = gender;
        this.neuter = neuter;
        this.specialMark = specialMark;
    }
}
