package com.hong.forapw.domain.group;

import com.hong.forapw.domain.District;
import com.hong.forapw.domain.Province;
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

    public boolean isCreator() {
        return groupRole == GroupRole.CREATOR;
    }

    public boolean isActiveMember() {
        return groupRole != GroupRole.TEMP;
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getUserNickname() {
        return user.getNickname();
    }

    public String getUserProfileURL() {
        return user.getProfileURL();
    }

    public String getUserEmail() {
        return user.getEmail();
    }

    public Province getUserProvince() {
        return user.getProvince();
    }

    public District getUserDistrict() {
        return user.getDistrict();
    }
}
