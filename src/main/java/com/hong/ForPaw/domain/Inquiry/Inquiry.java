package com.hong.ForPaw.domain.Inquiry;

import com.hong.ForPaw.domain.TimeStamp;
import com.hong.ForPaw.domain.User.User;
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
    private String contactMail;

    @Column
    private String imageURL;

    @Builder
    public Inquiry(User questioner, InquiryType type, InquiryStatus status, String title, String description, String contactMail, String imageURL) {
        this.questioner = questioner;
        this.type = type;
        this.status = status;
        this.title = title;
        this.description = description;
        this.contactMail = contactMail;
        this.imageURL = imageURL;
    }

    public void updateCustomerInquiry(String title, String description, String contactMail){
        this.title = title;
        this.description = description;
        this.contactMail = contactMail;
    }

    public void updateStatus(InquiryStatus status){
        this.status = status;
    }
}
