package com.hong.ForPaw.domain.Authentication;

import com.hong.ForPaw.domain.TimeStamp;
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

    @Column
    private Long userId;

    @Column
    private String clientIp;

    @Column
    private String userAgent;

    @Builder
    public LoginAttempt(Long userId, String clientIp, String userAgent) {
        this.userId = userId;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }
}
