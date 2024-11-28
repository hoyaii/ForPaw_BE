package com.hong.forapw.domain.faq;

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

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column
    @Enumerated(EnumType.STRING)
    private FaqType type;

    @Column
    private boolean isTop;

    @Builder
    public FAQ(String question, String answer, FaqType type, boolean isTop) {
        this.question = question;
        this.answer = answer;
        this.type = type;
        this.isTop = isTop;
    }
}
