package com.hong.ForPaw.domain.Inquiry;

import com.hong.ForPaw.domain.TimeStamp;
import com.hong.ForPaw.domain.User.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "answer_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Answer extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id")
    private Inquiry inquiry;

    @Column
    private String content;

    public Answer(User user, Inquiry inquiry, String content) {
        this.user = user;
        this.inquiry = inquiry;
        this.content = content;
    }
}
