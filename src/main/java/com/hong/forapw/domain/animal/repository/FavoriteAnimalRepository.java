package com.hong.forapw.domain.animal.repository;

import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.animal.entity.FavoriteAnimal;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteAnimalRepository extends JpaRepository<FavoriteAnimal, Long> {

    @Query("SELECT fa FROM FavoriteAnimal fa " +
            "JOIN fa.animal a " +
            "JOIN fa.user u " +
            "WHERE u.id = :userId AND a.id = :animalId AND a.removedAt IS NULL")
    Optional<FavoriteAnimal> findByUserIdAndAnimalId(Long userId, Long animalId);

    @Query("SELECT a FROM FavoriteAnimal fa " +
            "JOIN fa.animal a " +
            "JOIN fa.user u " +
            "WHERE u.id = :userId AND a.removedAt IS NULL")
    List<Animal> findAnimalsByUserId(@Param("userId") Long userId);

    @Query("SELECT a.id FROM FavoriteAnimal fa " +
            "JOIN fa.animal a " +
            "JOIN fa.user u " +
            "WHERE u.id = :userId AND a.removedAt IS NULL")
    List<Long> findAnimalIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM FavoriteAnimal fa WHERE fa.user.id = :userId AND fa.animal.removedAt IS NULL")
    void deleteAllByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM FavoriteAnimal fa WHERE fa.animal IN :animals AND fa.animal.removedAt IS NULL")
    void deleteByAnimalIn(@Param("animals") List<Animal> animals);
}
