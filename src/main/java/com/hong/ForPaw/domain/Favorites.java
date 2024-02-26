package com.hong.ForPaw.domain;

import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.domain.animal.Animal;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Favorites { //관심 동물

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @Builder
    public Favorites(User user, Animal animal) {
        this.user = user;
        this.animal = animal;
    }
}