package com.hong.ForPaw.domain.User;

import com.hong.ForPaw.controller.DTO.AuthenticationResponse;
import com.hong.ForPaw.domain.Authentication.Visit;
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
    private Long suspensionDays;

    @Column
    private String suspensionReason;

    @Builder
    public UserStatus(User user, boolean isActive, LocalDateTime suspensionStart, Long suspensionDays, String suspensionReason) {
        this.user = user;
        this.isActive = isActive;
        this.suspensionStart = suspensionStart;
        this.suspensionDays = suspensionDays;
        this.suspensionReason = suspensionReason;
    }

    public void updateForSuspend(AuthenticationResponse.UserBanDTO userBanDTO){
        this.isActive = false;
        this.suspensionStart = LocalDateTime.now();
        this.suspensionDays = userBanDTO.duration();
        this.suspensionReason = userBanDTO.reason();
    }

    public void updateForUnSuspend(){
        this.isActive = true;
        this.suspensionStart = null;
        this.suspensionDays = null;
        this.suspensionReason = null;

    public void updateIsActive(boolean isActive){
        this.isActive = isActive;
    }
}
