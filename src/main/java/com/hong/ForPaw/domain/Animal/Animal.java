package com.hong.ForPaw.domain.Animal;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String kind;

    @Column
    private String color;

    @Column
    private String age;

    @Column
    private String weight;

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
    public Animal(Long id, String kind, String color, String age, String weight, String profileURL, String processState, Gender gender, Boolean neuter, String specialMark, String noticeComment) {
        this.id = id;
        this.kind = kind;
        this.color = color;
        this.age = age;
        this.weight = weight;
        this.profileURL = profileURL;
        this.processState = processState;
        this.gender = gender;
        this.neuter = neuter;
        this.specialMark = specialMark;
        this.noticeComment = noticeComment;
    }
}
