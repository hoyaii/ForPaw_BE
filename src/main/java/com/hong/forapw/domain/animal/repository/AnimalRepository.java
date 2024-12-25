package com.hong.forapw.domain.animal.repository;

import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.animal.constant.AnimalType;
import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.shelter.Shelter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    @Query("SELECT a FROM Animal a WHERE a.removedAt IS NULL")
    Page<Animal> findAll(Pageable pageable);

    @Query("SELECT a.id FROM Animal a WHERE a.removedAt IS NULL")
    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT a.id FROM Animal a WHERE a.removedAt IS NULL")
    List<Long> findAllIds();

    @Query("SELECT a FROM Animal a WHERE (:category IS NULL OR a.category = :category) AND a.removedAt IS NULL")
    Page<Animal> findByAnimalType(@Param("category") AnimalType category, Pageable pageable);

    @Query("SELECT a FROM Animal a WHERE a.id = :id AND a.removedAt IS NULL")
    Optional<Animal> findById(@Param("id") Long id);

    @Query("SELECT a FROM Animal a WHERE a.id IN :ids AND a.removedAt IS NULL")
    List<Animal> findByIds(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"shelter"})
    @Query("SELECT a FROM Animal a WHERE a.id IN :ids AND a.removedAt IS NULL")
    List<Animal> findByIdsWithShelter(@Param("ids") List<Long> ids);

    @Query("SELECT a FROM Animal a " +
            "JOIN a.shelter s " +
            "WHERE (:category IS NULL OR a.category = :category) AND s.id = :careRegNo AND a.removedAt IS NULL")
    Page<Animal> findByShelterIdAndType(@Param("category") AnimalType category, @Param("careRegNo") Long careRegNo, Pageable pageable);

    @EntityGraph(attributePaths = {"shelter"})
    @Query("SELECT a FROM Animal a WHERE a.noticeEdt < :date AND a.removedAt IS NULL")
    List<Animal> findAllOutOfDateWithShelter(LocalDate date);

    @Query("SELECT a.id FROM Animal a " +
            "JOIN a.shelter s " +
            "JOIN s.regionCode r " +
            "WHERE a.removedAt IS NULL AND r.orgName = :district ORDER BY a.id ASC")
    List<Long> findIdsByDistrict(District district, Pageable pageable);

    @Query("SELECT a.id FROM Animal a " +
            "JOIN a.shelter s " +
            "JOIN s.regionCode r " +
            "WHERE a.removedAt IS NULL AND r.uprName = :province ORDER BY a.id ASC")
    List<Long> findIdsByProvince(Province province, Pageable pageable);

    List<Animal> findByShelter(Shelter shelter);

    @Query("SELECT COUNT(a) FROM Animal a " +
            "JOIN a.shelter s " +
            "WHERE s.id = :shelterId AND a.removedAt IS NULL")
    Long countByShelterId(@Param("shelterId") Long shelterId);

    @Query("SELECT COUNT(fa) FROM FavoriteAnimal fa WHERE fa.animal.id = :animalId")
    Long countLikesByAnimalId(@Param("animalId") Long animalId);

    @Query("SELECT COUNT(a) > 0 FROM Animal a WHERE a.id = :animalId AND a.removedAt IS NULL")
    boolean existsById(@Param("animalId") Long animalId);

    @Query("SELECT COUNT(a) FROM Animal a WHERE a.removedAt IS NULL")
    Long countAnimal();

    @Modifying
    @Transactional
    @Query("UPDATE Animal a SET a.shelter = :shelter WHERE a.id = :animalId")
    void updateShelter(@Param("shelter") Shelter shelter, @Param("animalId") Long animalId);

    @Modifying
    @Query("UPDATE Animal a SET a.shelter = :targetShelter WHERE a.shelter.id IN :shelterIds")
    void updateShelterByShelterIds(@Param("targetShelter") Shelter targetShelter, @Param("shelterIds") List<Long> shelterIds);
}
