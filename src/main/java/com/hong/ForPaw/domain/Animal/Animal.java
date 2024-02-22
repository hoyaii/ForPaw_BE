package com.hong.ForPaw.domain.Animal;

import com.hong.ForPaw.domain.Shelter.Shelter;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    @Enumerated(EnumType.STRING)
    private Gender gender;

    // 중성화 여부
    @Column
    private Boolean neuter;

    // 특징
    @Column
    private String specialMark;

    // 특이 사항
    @Column
    private String noticeComment;

    @Builder
    public Animal(Shelter shelter, LocalDate happenDt, String happenPlace, String kind, String color, String age, String weight, LocalDate noticeSdt, LocalDate noticeEdt, String profileURL, String processState, Gender gender, Boolean neuter, String specialMark, String noticeComment) {
        this.shelter = shelter;
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
        this.noticeComment = noticeComment;
    }
}
