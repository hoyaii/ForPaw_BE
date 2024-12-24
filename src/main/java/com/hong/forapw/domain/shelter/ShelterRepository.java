package com.hong.forapw.domain.shelter;

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

    @Query("SELECT s.id FROM Shelter s")
    List<Long> findAllIds();

    @Query("SELECT s FROM Shelter s WHERE s.animalCnt > 0 AND s.latitude IS NOT NULL AND s.isDuplicate = false ")
    List<Shelter> findAllWithAnimalAndLatitude();

    List<Shelter> findByAnimalCntGreaterThan(Long animalCnt);

    @Query(value = "SELECT * FROM shelter_tb WHERE MATCH(name, care_addr) AGAINST(:keyword IN BOOLEAN MODE) AND animal_cnt > 0 AND latitude IS NOT NULL AND is_duplicate = false",
            countQuery = "SELECT COUNT(*) FROM shelter_tb WHERE MATCH(name, care_addr) AGAINST(:keyword IN BOOLEAN MODE) AND animal_cnt > 0 AND latitude IS NOT NULL",
            nativeQuery = true)
    Page<Shelter> findByNameContaining(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"regionCode"})
    @Query("SELECT s FROM Shelter s")
    List<Shelter> findAllWithRegionCode();

    @Query(value = "SELECT s.*" +
            "FROM shelter_tb s " +
            "JOIN region_code_tb rc ON s.region_code_id = rc.id " +
            "WHERE s.animal_cnt >= 1 " +
            "ORDER BY (6371 * ACOS(COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) " +
            "* COS(RADIANS(s.longitude) - RADIANS(:lon)) + SIN(RADIANS(:lat)) " +
            "* SIN(RADIANS(s.latitude)))) ASC", nativeQuery = true)
    List<Shelter> findNearestShelters(@Param("lat") double lat, @Param("lon") double lon);

    @Query("SELECT s FROM Shelter s WHERE s.careTel IN " +
            "(SELECT s2.careTel FROM Shelter s2 GROUP BY s2.careTel, s2.latitude, s2.longitude HAVING COUNT(s2.id) > 1)")
    List<Shelter> findDuplicateShelters();

    @Query("SELECT s.careTel FROM Shelter s GROUP BY s.careTel HAVING COUNT(s) > 1")
    List<String> findDuplicateCareTels();

    List<Shelter> findByCareTel(String careTel);

    @Modifying
    @Transactional
    @Query("UPDATE Shelter s SET s.careTel = :careTel, s.careAddr = :careAddr, s.animalCnt = :animalCnt WHERE s.id = :shelterId")
    void updateShelterInfo(@Param("careTel") String careTel, @Param("careAddr") String careAddr, @Param("animalCnt") Long animalCnt, @Param("shelterId") Long shelterId);

    @Modifying
    @Transactional
    @Query("UPDATE Shelter s SET s.latitude = :latitude, s.longitude = :longitude WHERE s.id = :shelterId")
    void updateAddressInfo(@Param("latitude") Double latitude, @Param("longitude") Double longitude, @Param("shelterId") Long shelterId);

    @Modifying
    @Transactional
    @Query("UPDATE Shelter s SET s.isDuplicate = :isDuplicate WHERE s.id = :shelterId")
    void updateIsDuplicate(@Param("shelterId") Long shelterId, @Param("isDuplicate") boolean isDuplicate);

    @Modifying
    @Query("UPDATE Shelter s SET s.isDuplicate = :isDuplicate WHERE s.id IN :shelterIds")
    void updateIsDuplicateByIds(@Param("shelterIds") List<Long> shelterIds, @Param("isDuplicate") boolean isDuplicate);
}