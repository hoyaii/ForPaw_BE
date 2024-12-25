package com.hong.forapw.domain.alarm.entity;

import com.hong.forapw.domain.alarm.constant.AlarmType;
import com.hong.forapw.common.entity.BaseEntity;
import com.hong.forapw.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alarm_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Alarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User receiver;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column
    private String redirectURL;

    @Column
    private Boolean isRead = false;

    @Column
    private LocalDateTime readDate;

    // "ALTER TABLE alarm_tb MODIFY COLUMN alarm_type VARCHAR(20)"을 통해 늘려야함
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;

    @Builder
    public Alarm(User receiver, String content, String redirectURL, AlarmType alarmType) {
        this.receiver = receiver;
        this.content = content;
        this.redirectURL = redirectURL;
        this.alarmType = alarmType;
    }

    public void updateIsRead(Boolean isRead, LocalDateTime readDate) {
        this.isRead = isRead;
        this.readDate = readDate;
    }

    public Long getReceiverId() {
        return receiver.getId();
    }
}