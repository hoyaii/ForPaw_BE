package com.hong.ForPaw.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "faq_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class FAQ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition="TEXT")
    private String question;

    @Column(columnDefinition="TEXT")
    private String answer;

    @Column
    private String type;

    @Builder
    public FAQ(String question, String answer, String type) {
        this.question = question;
        this.answer = answer;
        this.type = type;
    }
}
