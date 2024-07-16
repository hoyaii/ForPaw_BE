package com.hong.ForPaw.repository;

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

    @Query("SELECT a FROM Apply a WHERE a.user.id = :userId AND a.removedAt IS NULL")
    List<Apply> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) FROM Apply a WHERE a.user.id = : userId AND a.status = 'PROCESSED' ")
    Long countByUserIdProcessed(@Param("userId") Long userId);
    @Query("SELECT COUNT(a) FROM Apply a WHERE a.user.id = : userId AND a.status = 'PROCESSING' ")
    Long countByUserIdProcessing(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) FROM Apply a WHERE a.status = 'PROCESSING' AND a.removedAt IS NULL")
    Long countProcessing();

    @Query("SELECT COUNT(a) FROM Apply a WHERE a.status = 'PROCESSING' AND a.updatedDate >= :date AND a.removedAt IS NULL")
    Long countProcessingWithinDate(LocalDateTime date);

    @Query("SELECT COUNT(a) FROM Apply a WHERE a.status = 'PROCESSED' AND a.removedAt IS NULL")
    Long countProcessed();

    @Query("SELECT COUNT(a) FROM Apply a WHERE a.status = 'PROCESSED' AND a.updatedDate >= :date AND a.removedAt IS NULL")
    Long countProcessedWithinDate(LocalDateTime date);

    @Query("SELECT COUNT(a) > 0 FROM Apply a WHERE a.id = :applyId AND a.user.id = :userId AND a.removedAt IS NULL")
    boolean existsByApplyIdAndUserId(@Param("applyId") Long applyId, @Param("userId") Long userId);

    @Query("SELECT COUNT(a) > 0 FROM Apply a WHERE a.user.id = :userId AND a.animal.id = :animalId AND a.removedAt IS NULL")
    boolean existsByUserIdAndAnimalId(@Param("userId") Long userId, @Param("animalId") Long animalId);

    @Query("SELECT a.animal.id FROM Apply a WHERE a.id = :applyId AND a.removedAt IS NULL")
    Long findAnimalIdById(@Param("applyId") Long applyId);

    void deleteAllByUserId(Long userId);
    @EntityGraph(attributePaths = {"animal"})
    @Query("SELECT a From Apply a WHERE a.removedAt IS NULL")
    Page<Apply> findAllWithAnimal(Pageable pageable);

    @EntityGraph(attributePaths = {"animal"})
    @Query("SELECT a From Apply a WHERE a.status = :applyStatus AND a.removedAt IS NULL")
    Page<Apply> findProcessApply(Pageable pageable, @Param("applyStatus") ApplyStatus applyStatus);
}
