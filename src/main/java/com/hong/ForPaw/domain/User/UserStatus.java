package com.hong.ForPaw.domain.User;

import com.hong.ForPaw.domain.Authentication.Visit;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import lombok.Setter;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

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

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private Visit visit;

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

    public void UpdatesuspensionReason(String suspensionReason){
        this.suspensionReason = suspensionReason;
    }

    public void UpdatesuspensionDays(Long suspensionDays){
        this.suspensionDays = suspensionDays;
    }

    public void UpdatesuspensionStart(LocalDateTime suspensionStart){
        this.suspensionStart = suspensionStart;
    }
    public void UpdateisActive(Boolean isActive){
        this.isActive = isActive;
    }
}
