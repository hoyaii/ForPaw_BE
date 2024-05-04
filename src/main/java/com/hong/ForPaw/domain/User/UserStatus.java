package com.hong.ForPaw.domain.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_status_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class UserStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private boolean isActive;

    @Column
    private LocalDateTime suspensionStart;

    @Column
    private Integer suspensionDays;

    @Builder
    public UserStatus(User user, boolean isActive, LocalDateTime suspensionStart, Integer suspensionDays) {
        this.user = user;
        this.isActive = isActive;
        this.suspensionStart = suspensionStart;
        this.suspensionDays = suspensionDays;
    }
}
