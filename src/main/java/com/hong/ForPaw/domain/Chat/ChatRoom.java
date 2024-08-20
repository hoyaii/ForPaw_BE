package com.hong.ForPaw.domain.Chat;

import com.hong.ForPaw.domain.Group.Group;
import com.hong.ForPaw.domain.TimeStamp;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chatRoom_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ChatRoom extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column
    private String name;

    @Builder
    public ChatRoom(Group group, String name) {
        this.group = group;
        this.name = name;
    }

    public void updateName(String name){
        this.name = name;
    }
}