package com.hong.ForPaw.domain.Inquiry;

import com.hong.ForPaw.domain.TimeStamp;
import com.hong.ForPaw.domain.User.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inquiry_answer_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class InquiryAnswer extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User answerer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id")
    private Inquiry inquiry;

    @Column
    private String content;

    @Builder
    public InquiryAnswer(User answerer, Inquiry inquiry, String content) {
        this.answerer = answerer;
        this.inquiry = inquiry;
        this.content = content;
    }
}
