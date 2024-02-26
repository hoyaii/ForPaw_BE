package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Favorite;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.domain.animal.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndAnimal(User user, Animal animal);
}
