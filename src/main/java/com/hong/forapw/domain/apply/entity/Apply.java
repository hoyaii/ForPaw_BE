package com.hong.forapw.domain.apply.entity;

import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.apply.constant.ApplyStatus;
import com.hong.forapw.common.entity.BaseEntity;
import com.hong.forapw.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;

@Entity
@Table(name = "apply_tb")
@SQLDelete(sql = "UPDATE apply_tb SET removed_at = NOW() WHERE id=?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Apply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @Column
    @Enumerated(EnumType.STRING)
    private ApplyStatus status;

    @Column
    private String name;

    @Column
    private String tel;

    @Column
    private String roadNameAddress;

    @Column
    private String addressDetail;

    @Column
    private String zipCode;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Builder
    public Apply(User user, Animal animal, ApplyStatus status, String name, String tel, String roadNameAddress, String addressDetail, String zipCode) {
        this.user = user;
        this.animal = animal;
        this.status = status;
        this.name = name;
        this.tel = tel;
        this.addressDetail = addressDetail;
        this.roadNameAddress = roadNameAddress;
        this.zipCode = zipCode;
    }

    public void updateApply(String name, String tel, String roadNameAddress, String addressDetail, String zipCode) {
        this.name = name;
        this.tel = tel;
        this.roadNameAddress = roadNameAddress;
        this.addressDetail = addressDetail;
        this.zipCode = zipCode;
    }

    public void updateApplyStatus(ApplyStatus applyStatus) {
        this.status = applyStatus;
    }
}
