package com.hong.forapw.domain.chat.entity;

import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chatRoom_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ChatRoom extends BaseEntity {

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

    public void updateName(String name) {
        this.name = name;
    }
}