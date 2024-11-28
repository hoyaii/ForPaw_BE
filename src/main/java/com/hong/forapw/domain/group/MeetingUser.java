package com.hong.forapw.domain.group;

import com.hong.forapw.domain.TimeStamp;
import com.hong.forapw.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "meetingUser_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MeetingUser extends TimeStamp {

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
