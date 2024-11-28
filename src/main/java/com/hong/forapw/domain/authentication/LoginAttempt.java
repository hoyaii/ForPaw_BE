package com.hong.forapw.domain.authentication;

import com.hong.forapw.domain.TimeStamp;
import com.hong.forapw.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "login_attempt_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class LoginAttempt extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private String clientIp;

    @Column
    private String userAgent;

    @Builder
    public LoginAttempt(User user, String clientIp, String userAgent) {
        this.user = user;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }
}
