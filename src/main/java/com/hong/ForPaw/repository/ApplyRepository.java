package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Apply.Apply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplyRepository extends JpaRepository<Apply, Long> {

    @Query("SELECT a FROM Apply a WHERE a.user.id = :userId")
    List<Apply> findByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Apply a Where a.user.id = :userId AND a.animal.id = :animalId")
    Optional<Apply> findByUserIdAndAnimalId(@Param("userId") Long userId, @Param("animalId") Long animalId);
}
