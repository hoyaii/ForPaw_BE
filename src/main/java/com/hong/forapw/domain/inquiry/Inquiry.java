package com.hong.forapw.domain.inquiry;

import com.hong.forapw.domain.TimeStamp;
import com.hong.forapw.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inquiry_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Inquiry extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questioner_id")
    private User questioner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User answerer;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private InquiryType type;

    @Column
    @Enumerated(EnumType.STRING)
    private InquiryStatus status;

    @Column
    private String title;

    @Column
    private String description;

    @Column
    private String answer;

    @Column
    private String contactMail;

    @Column
    private String imageURL;

    @Builder
    public Inquiry(User questioner, User answerer, InquiryType type, InquiryStatus status, String title, String description, String contactMail, String imageURL, String answer) {
        this.questioner = questioner;
        this.answerer = answerer;
        this.type = type;
        this.status = status;
        this.title = title;
        this.description = description;
        this.contactMail = contactMail;
        this.imageURL = imageURL;
        this.answer = answer;
    }

    public void updateAnswer(String answer, User answerer) {
        this.answer = answer;
        this.answerer = answerer;
    }

    public void updateInquiry(String title, String description, String contactMail) {
        this.title = title;
        this.description = description;
        this.contactMail = contactMail;
    }

    public void updateStatus(InquiryStatus status) {
        this.status = status;
    }
}
