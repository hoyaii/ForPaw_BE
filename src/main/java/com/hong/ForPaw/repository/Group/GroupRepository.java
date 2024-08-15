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

    @Query("SELECT g FROM Group g WHERE g.province = :province AND g.district = :district " +
            "AND (:userId IS NULL OR g.id NOT IN " +
            "(SELECT g.id FROM GroupUser gu JOIN gu.group g JOIN gu.user u WHERE u.id = :userId))")
    Page<Group> findByDistrictAndSubDistrict(@Param("province") Province province,
                                             @Param("district") District district,
                                             @Param("userId") Long userId,
                                             Pageable pageable);

    @Query("SELECT g FROM Group g WHERE g.province = :province")
    Page<Group> findByProvince(@Param("province") Province province, Pageable pageable);

    @Query(value = "SELECT * FROM groups_tb WHERE MATCH(name) AGAINST(:name IN BOOLEAN MODE)",
            countQuery = "SELECT COUNT(*) FROM groups_tb WHERE MATCH(name) AGAINST(:name IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<Group> findByNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT g.id FROM Group g")
    List<Long> findAllIds();

    boolean existsByName(String name);

    @Query("SELECT COUNT(g) > 0 FROM Group g WHERE g.id != :id AND g.name = :name")
    boolean existsByNameExcludingId(@Param("name") String name, @Param("id") Long id);

    @Modifying
    @Query("UPDATE Group g SET g.likeNum = :likeNum WHERE g.id = :groupId")
    void updateLikeNum(@Param("likeNum") Long likeNum, @Param("groupId") Long groupId);
}
