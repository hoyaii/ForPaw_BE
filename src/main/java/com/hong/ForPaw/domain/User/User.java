package com.hong.ForPaw.domain.User;

import com.hong.ForPaw.domain.TimeStamp;
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

    @Column
    private String name;

    @Column
    private String nickName;

    @Column
    private String email;

    @Column
    private String password;

    @Column
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column
    private String profileURL;

    // 활동 지역 - 시/도
    @Column
    private String state;

    // 활동 지역 - 군/구/시
    @Column
    private String district;

    // 활동 지역 - 동/읍/면
    @Column
    private String subDistrict;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Builder
    public User(Long id, String name, String nickName, String email, String password, Role role, String profileURL, String state, String district, String subDistrict) {
        this.id = id;
        this.name = name;
        this.nickName = nickName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.profileURL = profileURL;
        this.state = state;
        this.district = district;
        this.subDistrict = subDistrict;
    }

    public void updatePassword (String password) {
        this.password  = password;
    }

    public void updateProfile(String nickName, String province, String district, String subDistrict,String profileURL){
        this.nickName = nickName;
        this.state = province;
        this.district = district;
        this.subDistrict = subDistrict;
        this.profileURL = profileURL;
    }

    public void updateRole(Role role){ this.role = role; }
}
