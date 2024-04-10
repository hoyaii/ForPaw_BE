package com.hong.ForPaw.repository.Group;

import com.hong.ForPaw.domain.Group.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g WHERE g.district = :district AND g.subDistrict = :subDistrict")
    Page<Group> findByDistrictAndSubDistrict(@Param("district") String district, @Param("subDistrict") String subDistrict, Pageable pageable);

    @Query("SELECT g FROM Group g WHERE g.district = :district")
    Page<Group> findByDistrict(@Param("district") String district, Pageable pageable);

    boolean existsByName(String name);

    @Query("SELECT COUNT(g) > 0 FROM Group g WHERE g.id != :id AND g.name = :name")
    boolean existsByNameExcludingId(@Param("name") String name, @Param("id") Long id);

    @Query(value = "SELECT * FROM groups_tb WHERE MATCH(name) AGAINST(:name IN BOOLEAN MODE)", nativeQuery = true)
    List<Group> findByNameContaining(@Param("name") String name);

    @Query("SELECT g.id FROM Group g")
    Page<Long> findGroupIds(Pageable pageable);

    @Modifying
    @Query("UPDATE Group g SET g.participantNum = g.participantNum + 1 WHERE g.id = :groupId")
    void incrementParticipantNum(@Param("groupId") Long groupId);

    @Modifying
    @Query("UPDATE Group g SET g.participantNum = g.participantNum - 1 WHERE g.id = :groupId AND g.participantNum > 0")
    void decrementParticipantNum(@Param("groupId") Long groupId);

    @Modifying
    @Query("UPDATE Group g SET g.likeNum = :likeNum WHERE g.id = :groupId")
    void updateLikeNum(@Param("likeNum") Long likeNum, @Param("groupId") Long groupId);
}
