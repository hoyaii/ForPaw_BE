package com.hong.ForPaw.repository.Group;

import com.hong.ForPaw.domain.Group.Group;
import com.hong.ForPaw.domain.District;
import com.hong.ForPaw.domain.Province;
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

    @Query("SELECT g FROM Group g WHERE g.province = :province AND g.district = :district")
    Page<Group> findByDistrictAndSubDistrict(@Param("province") Province province, @Param("district") District district, Pageable pageable);

    @Query("SELECT g FROM Group g WHERE g.province = :province")
    Page<Group> findByProvince(@Param("province") Province province, Pageable pageable);

    boolean existsByName(String name);

    @Query("SELECT COUNT(g) > 0 FROM Group g WHERE g.id != :id AND g.name = :name")
    boolean existsByNameExcludingId(@Param("name") String name, @Param("id") Long id);

    @Query(value = "SELECT * FROM groups_tb WHERE MATCH(name) AGAINST(:name IN BOOLEAN MODE)", nativeQuery = true)
    List<Group> findByNameContaining(@Param("name") String name);

    @Query("SELECT g.id FROM Group g")
    List<Long> findGroupIds();

    @Modifying
    @Query("UPDATE Group g SET g.participantNum = g.participantNum - 1 WHERE g.id = :groupId AND g.participantNum > 0")
    void decrementParticipantNum(@Param("groupId") Long groupId);

    @Modifying
    @Query("UPDATE Group g SET g.likeNum = :likeNum WHERE g.id = :groupId")
    void updateLikeNum(@Param("likeNum") Long likeNum, @Param("groupId") Long groupId);
}
