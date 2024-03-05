package com.hong.ForPaw.domain.Group;

import com.hong.ForPaw.domain.TimeStamp;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Meeting extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column
    private String name;

    @Column
    private LocalDateTime date;

    @Column
    private String location;

    @Column
    private Long cost;

    @Column
    private Integer participantNum;

    @Column
    private Integer maxNum;

    @Column
    private String description;

    @Column
    private String profileURL;

    @Builder
    public Meeting(Group group, String name, LocalDateTime date, String location, Long cost, Integer participantNum, Integer maxNum, String description, String profileURL) {
        this.group = group;
        this.name = name;
        this.date = date;
        this.location = location;
        this.cost = cost;
        this.participantNum = participantNum;
        this.maxNum = maxNum;
        this.description = description;
        this.profileURL = profileURL;
    }
}
