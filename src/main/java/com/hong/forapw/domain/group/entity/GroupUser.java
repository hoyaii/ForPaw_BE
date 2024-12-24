package com.hong.forapw.domain.group.entity;

import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.common.entity.BaseEntity;
import com.hong.forapw.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "groupUser_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class GroupUser extends BaseEntity {

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

    public boolean isMember() {
        return groupRole.equals(GroupRole.USER) || groupRole.equals(GroupRole.ADMIN) || groupRole.equals(GroupRole.CREATOR);
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
