package com.hong.forapw.domain.animal;

import com.hong.forapw.domain.TimeStamp;
import com.hong.forapw.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "favoriteAnimal_tb", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "animal_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class FavoriteAnimal extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @Builder
    public FavoriteAnimal(User user, Animal animal) {
        this.user = user;
        this.animal = animal;
    }
}