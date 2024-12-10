package com.hong.forapw.domain.user;

import com.hong.forapw.domain.District;
import com.hong.forapw.domain.Province;
import com.hong.forapw.domain.TimeStamp;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_tb")
@SQLDelete(sql = "UPDATE user_tb SET removed_at = NOW() WHERE id=?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "user")
    private UserStatus status;

    @Column
    private String name;

    @Column
    private String nickname;

    @Column
    private String email;

    @Column
    private String password;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column
    private String profileURL;

    @Column
    @Enumerated(EnumType.STRING)
    private Province province; // 활동 지역 - 시/도

    @Column
    @Enumerated(EnumType.STRING)
    private District district; // 활동 지역 - 군/구/시

    @Column
    private String subDistrict; // 활동 지역 - 동/읍/면

    @Column
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    @Column
    private boolean isMarketingAgreed;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Builder
    public User(Long id, UserStatus status, String name, String nickName, String email, String password, UserRole role, String profileURL, Province province, District district, String subDistrict, AuthProvider authProvider, boolean isMarketingAgreed) {
        this.id = id;
        this.status = status;
        this.name = name;
        this.nickname = nickName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.profileURL = profileURL;
        this.province = province;
        this.district = district;
        this.subDistrict = subDistrict;
        this.authProvider = authProvider;
        this.isMarketingAgreed = isMarketingAgreed;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateProfile(String nickName, Province province, District district, String subDistrict, String profileURL) {
        this.nickname = nickName;
        this.province = province;
        this.district = district;
        this.subDistrict = subDistrict;
        this.profileURL = profileURL;
    }

    public void deactivateUser() {
        status.updateActiveness(false);
    }

    public void updateRole(UserRole role) {
        this.role = role;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isExitMember() {
        return removedAt != null;
    }

    public boolean isUnActive() {
        return !status.isActive();
    }

    public boolean isLocalJoined() {
        return authProvider.equals(AuthProvider.LOCAL);
    }

    public boolean isSocialJoined() {
        return !authProvider.equals(AuthProvider.LOCAL);
    }

    public boolean isShelterOwns() {
        return role.equals(UserRole.SHELTER);
    }

    public boolean isNickNameUnequal(String nickname) {
        return !this.nickname.equals(nickname);
    }

    public boolean isNotSameUser(Long userId) {
        return !this.id.equals(userId);
    }

    public boolean isSameUser(Long userId) {
        return this.id.equals(userId);
    }

    public boolean isAdmin() {
        return role.equals(UserRole.ADMIN) || role.equals(UserRole.SUPER);
    }
}
