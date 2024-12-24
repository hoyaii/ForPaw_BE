package com.hong.forapw.domain.group.entity;

import com.hong.forapw.common.entity.BaseEntity;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "groups_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Group extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    @Enumerated(EnumType.STRING)
    private Province province;

    @Column
    @Enumerated(EnumType.STRING)
    private District district;

    @Column
    private String subDistrict;

    @Column
    private String description;

    @Column
    private String category;

    @Column
    private String profileURL;

    @Column
    private Long maxNum = 30L;

    @Column
    private Long participantNum = 0L;

    @Column
    private boolean isShelterOwns;

    @Column
    private String shelterName;

    @Builder
    public Group(String name, Province province, District district, String subDistrict, String description, String category, String profileURL, Long maxNum, boolean isShelterOwns, String shelterName) {
        this.name = name;
        this.province = province;
        this.district = district;
        this.subDistrict = subDistrict;
        this.description = description;
        this.category = category;
        this.profileURL = profileURL;
        this.maxNum = maxNum;
        this.isShelterOwns = isShelterOwns;
        this.shelterName = shelterName;
    }

    public void updateInfo(String name, Province province, District district, String subDistrict, String description, String category, String profileURL, Long maxNum) {
        this.name = name;
        this.province = province;
        this.district = district;
        this.subDistrict = subDistrict;
        this.description = description;
        this.category = category;
        this.profileURL = profileURL;
        this.maxNum = maxNum;
    }

    public void incrementParticipantNum() {
        this.participantNum = this.participantNum + 1;
    }

    public void decrementParticipantNum() {
        this.participantNum = this.participantNum - 1;
    }
}
