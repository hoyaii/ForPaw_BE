package com.hong.ForPaw.domain.Group;

import com.hong.ForPaw.domain.User.User;
import lombok.Getter;

import javax.persistence.*;

@Entity
@Getter
public class GroupUser {

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
    private Role role;
}
