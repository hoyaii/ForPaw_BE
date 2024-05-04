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
public class CustomerInquiry extends TimeStamp {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private String title;

    @Column
    private String description;

    @Column
    private String contactMail;

    @Column
    @Enumerated(EnumType.STRING)
    private Status status;

    @Builder
    public CustomerInquiry(User user, String title, String description, String contactMail, Status status) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.contactMail = contactMail;
        this.status = status;
    }

    public void updateCustomerInquiry(String title, String description, String contactMail){
        this.title = title;
        this.description = description;
        this.contactMail = contactMail;
    }
}
