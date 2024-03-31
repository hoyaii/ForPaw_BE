package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Shelter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    @Modifying
    @Query("DELETE FROM Shelter s WHERE s.animalCnt = 0")
    void deleteZeroShelter();

    List<Shelter> findByAnimalCntGreaterThan(Long animalCnt);

    Page<Shelter> findByNameContaining(@Param("name") String name, Pageable pageable);

    @EntityGraph(attributePaths = {"regionCode"})
    @Query("SELECT s FROM Shelter s")
    List<Shelter> findAllWithRegionCode();

    @Modifying
    @Transactional
    @Query("UPDATE Shelter s SET s.careTel = :careTel, s.careAddr = :careAddr, s.animalCnt = :animalCnt WHERE s.id = :shelterId")
    void updateShelterInfo(@Param("careTel") String careTel, @Param("careAddr") String careAddr, @Param("animalCnt") Long animalCnt, @Param("shelterId") Long shelterId);

    @Modifying
    @Transactional
    @Query("UPDATE Shelter s SET s.latitude = :latitude, s.longitude = :longitude WHERE s.id = :shelterId")
    void updateAddressInfo(@Param("latitude") Double latitude, @Param("longitude") Double longitude, @Param("shelterId") Long shelterId);

    @Query(value = "SELECT * FROM shelter_tb " +
            "WHERE animal_cnt >= 1 " +
            "ORDER BY (6371 * ACOS(COS(RADIANS(:lat)) * COS(RADIANS(latitude)) " +
            "* COS(RADIANS(longitude) - RADIANS(:lon)) + SIN(RADIANS(:lat)) " +
            "* SIN(RADIANS(latitude)))) ASC",
            nativeQuery = true)
    List<Shelter> findNearestShelters(@Param("lat") double lat, @Param("lon") double lon);
}