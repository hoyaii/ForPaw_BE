package com.hong.ForPaw.domain.FAQ;

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
    private FaqType type;

    @Builder
    public FAQ(String question, String answer, FaqType type) {
        this.question = question;
        this.answer = answer;
        this.type = type;
    }
}
