package com.hong.ForPaw.domain.Group;

import com.hong.ForPaw.domain.TimeStamp;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "groups_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Group extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private String district;

    @Column
    private String subDistrict;

    @Column
    private String description;

    @Column
    private String category;

    @Column
    private String profileURL;

    @Column
    private Long participantNum = 0L;

    @Column
    private Long likeNum = 0L;

    @Builder
    public Group(String name, String district, String subDistrict, String description, String category, String profileURL) {
        this.name = name;
        this.district = district;
        this.subDistrict = subDistrict;
        this.description = description;
        this.category = category;
        this.profileURL = profileURL;
    }

    public void updateInfo(String name, String district, String subDistrict, String description, String category, String profileURL){
        this.name = name;
        this.district = district;
        this.subDistrict = subDistrict;
        this.description = description;
        this.category = category;
        this.profileURL = profileURL;
    }
}
