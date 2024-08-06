package com.hong.ForPaw.domain.Alarm;

import com.hong.ForPaw.domain.TimeStamp;
import com.hong.ForPaw.domain.User.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "alarm_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Alarm extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User receiver;

    @Column(columnDefinition="TEXT")
    private String content;

    @Column
    private String redirectURL;

    @Column
    private Boolean isRead = false;

    @Column
    private LocalDateTime readDate;

    @Column
    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;

    @Builder
    public Alarm(User receiver, String content, String redirectURL, AlarmType alarmType) {
        this.receiver = receiver;
        this.content = content;
        this.redirectURL = redirectURL;
        this.alarmType = alarmType;
    }

    public void updateIsRead(Boolean isRead, LocalDateTime readDate){
        this.isRead = isRead;
        this.readDate = readDate;
    }
}