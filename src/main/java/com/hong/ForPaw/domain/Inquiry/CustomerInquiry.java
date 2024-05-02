package com.hong.ForPaw.domain.Inquiry;

import com.hong.ForPaw.domain.User.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inquiry_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerInquiry {

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

    public CustomerInquiry(User user, String title, String description, String contactMail) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.contactMail = contactMail;
    }
}
