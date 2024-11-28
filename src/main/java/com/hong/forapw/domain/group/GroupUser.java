package com.hong.forapw.domain.group;

import com.hong.forapw.domain.TimeStamp;
import com.hong.forapw.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "groupUser_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class GroupUser extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    @Enumerated(EnumType.STRING)
    private GroupRole groupRole;

    @Column
    private String greeting;

    @Builder
    public GroupUser(Group group, User user, GroupRole groupRole, String greeting) {
        this.group = group;
        this.user = user;
        this.groupRole = groupRole;
        this.greeting = greeting;
    }

    public void updateRole(GroupRole groupRole) {
        this.groupRole = groupRole;
    }
}
