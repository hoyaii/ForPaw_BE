package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Shelter;
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

    @Query("SELECT s.id FROM Shelter s")
    List<Long> findAllIds();

    @Query("SELECT s FROM Shelter s WHERE s.animalCnt > 0 AND s.latitude IS NOT NULL")
    List<Shelter> findAllWithAnimalAndLatitude();

    List<Shelter> findByAnimalCntGreaterThan(Long animalCnt);

    @Query(value = "SELECT * FROM shelter_tb WHERE MATCH(name, care_addr) AGAINST(:keyword IN BOOLEAN MODE)", nativeQuery = true)
    List<Shelter> findByNameContaining(@Param("keyword") String keyword);

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

    @Query(value = "SELECT s.*" +
            "FROM shelter_tb s " +
            "JOIN region_code_tb rc ON s.region_code_id = rc.id " +
            "WHERE s.animal_cnt >= 1 " +
            "ORDER BY (6371 * ACOS(COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) " +
            "* COS(RADIANS(s.longitude) - RADIANS(:lon)) + SIN(RADIANS(:lat)) " +
            "* SIN(RADIANS(s.latitude)))) ASC", nativeQuery = true)
    List<Shelter> findNearestShelters(@Param("lat") double lat, @Param("lon") double lon);

    @Modifying
    @Transactional
    @Query("DELETE FROM Shelter s WHERE s.animalCnt = 0")
    void deleteSheltersWithZeroAnimalCnt();
}