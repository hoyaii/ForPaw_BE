package com.hong.ForPaw.domain.User;

import com.hong.ForPaw.domain.District;
import com.hong.ForPaw.domain.Province;
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

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private UserStatus status;

    @Column
    private String name;

    @Column
    private String nickName;

    @Column
    private String email;

    @Column
    private String password;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column
    private String profileURL;

    // 활동 지역 - 시/도
    @Column
    @Enumerated(EnumType.STRING)
    private Province province;

    // 활동 지역 - 군/구/시
    @Column
    @Enumerated(EnumType.STRING)
    private District  district;

    // 활동 지역 - 동/읍/면
    @Column
    private String subDistrict;

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
        this.nickName = nickName;
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

    public void updatePassword (String password) {
        this.password  = password;
    }

    public void updateProfile(String nickName, Province province, District district, String subDistrict, String profileURL){
        this.nickName = nickName;
        this.province = province;
        this.district = district;
        this.subDistrict = subDistrict;
        this.profileURL = profileURL;
    }

    public void updateRole(UserRole role){ this.role = role; }

    public void updateStatus(UserStatus status){ this.status = status; }
}
