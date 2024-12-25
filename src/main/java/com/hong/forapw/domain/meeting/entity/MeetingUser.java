package com.hong.forapw.domain.meeting.entity;

import com.hong.forapw.common.entity.BaseEntity;
import com.hong.forapw.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "meetingUser_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MeetingUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public MeetingUser(User user) {
        this.user = user;
    }

    public void updateMeeting(Meeting meeting) {
        this.meeting = meeting;
    }
}
