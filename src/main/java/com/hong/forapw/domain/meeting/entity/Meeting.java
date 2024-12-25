package com.hong.forapw.domain.meeting.entity;

import com.hong.forapw.common.entity.BaseEntity;
import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meeting_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Meeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User creator; // 주최자

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 30)
    private List<MeetingUser> meetingUsers = new ArrayList<>();

    @Column
    private String name;

    @Column
    private LocalDateTime meetDate;

    @Column
    private String location;

    @Column
    private Long cost;

    @Column
    private Integer maxNum;

    @Column
    private String description;

    @Column
    private String profileURL;

    @Column
    private Long participantNum = 0L;

    @Builder
    public Meeting(Group group, User creator, String name, LocalDateTime meetDate, String location, Long cost, Integer maxNum, String description, String profileURL) {
        this.group = group;
        this.creator = creator;
        this.name = name;
        this.meetDate = meetDate;
        this.location = location;
        this.cost = cost;
        this.maxNum = maxNum;
        this.description = description;
        this.profileURL = profileURL;
    }

    public void updateMeeting(String name, LocalDateTime meetDate, String location, Long cost, Integer maxNum, String description, String profileURL) {
        this.name = name;
        this.meetDate = meetDate;
        this.location = location;
        this.cost = cost;
        this.maxNum = maxNum;
        this.description = description;
        this.profileURL = profileURL;
    }

    public void addMeetingUser(MeetingUser meetingUser) {
        meetingUsers.add(meetingUser);
        meetingUser.updateMeeting(this);
    }

    public void incrementParticipantCount() {
        participantNum++;
    }

    public void decrementParticipantCount() {
        participantNum--;
    }

    public String getCreatorNickName() {
        return creator.getNickname();
    }
}
