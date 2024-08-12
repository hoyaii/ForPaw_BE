package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Animal.Animal;
import com.hong.ForPaw.domain.Apply.Apply;
import com.hong.ForPaw.domain.Apply.ApplyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplyRepository extends JpaRepository<Apply, Long> {

    @Query("SELECT a FROM Apply a WHERE a.removedAt IS NULL")
    List<Apply> findAll();

    @Query("SELECT a FROM Apply a WHERE a.id = :id AND a.removedAt IS NULL")
    Optional<Apply> findById(@Param("id") Long id);

    @Query("SELECT a FROM Apply a " +
            "JOIN a.user u " +
            "WHERE u.id = :userId AND a.removedAt IS NULL")
    List<Apply> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Apply a WHERE a.status = 'PROCESSING' AND a.removedAt IS NULL")
    List<Apply> findAllProcessing();

    @Query("SELECT an FROM Apply a " +
            "JOIN a.animal an " +
            "WHERE a.id = :applyId AND a.removedAt IS NULL")
    Optional<Animal> findAnimalIdById(@Param("applyId") Long applyId);

    @EntityGraph(attributePaths = {"animal"})
    @Query("SELECT a From Apply a WHERE (:status IS NULL OR a.status = :status) AND a.removedAt IS NULL")
    Page<Apply> findAllByStatusWithAnimal(@Param("status") ApplyStatus status, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Apply a WHERE a.status = :applyStatus AND a.removedAt IS NULL")
    Long countByStatus(@Param("applyStatus") ApplyStatus applyStatus);

    @Query("SELECT COUNT(a) FROM Apply a WHERE a.status = :applyStatus AND a.updatedDate >= :date AND a.removedAt IS NULL")
    Long countByStatusWithinDate(@Param("applyStatus") ApplyStatus applyStatus, @Param("date") LocalDateTime date);

    @Query("SELECT COUNT(a) > 0 FROM Apply a " +
            "JOIN a.user u " +
            "WHERE a.id = :applyId AND u.id = :userId AND a.removedAt IS NULL")
    boolean existsByApplyIdAndUserId(@Param("applyId") Long applyId, @Param("userId") Long userId);

    @Query("SELECT COUNT(a) > 0 FROM Apply a " +
            "JOIN a.user u " +
            "JOIN a.animal an " +
            "WHERE u.id = :userId AND an.id = :animalId AND a.removedAt IS NULL")
    boolean existsByUserIdAndAnimalId(@Param("userId") Long userId, @Param("animalId") Long animalId);
}
