package com.hong.ForPaw.domain.Group;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private String region;

    @Column
    private String subRegion;

    @Column
    private String description;

    @Column
    private String category;

    @Column
    private String profileURL;

    @Column
    private Integer likeNum = 0;

    @Builder
    public Group(String name, String region, String subRegion, String description, String category, String profileURL, Integer likeNum) {
        this.name = name;
        this.region = region;
        this.subRegion = subRegion;
        this.description = description;
        this.category = category;
        this.profileURL = profileURL;
        this.likeNum = likeNum;
    }
}
