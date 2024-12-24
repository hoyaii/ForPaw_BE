package com.hong.forapw.domain.group.repository;

import com.hong.forapw.domain.group.entity.FavoriteGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteGroupRepository extends JpaRepository<FavoriteGroup, Long> {

    Optional<FavoriteGroup> findByUserIdAndGroupId(Long userId, Long groupId);

    @Query("SELECT g.id FROM FavoriteGroup fg " +
            "JOIN fg.group g " +
            "JOIN fg.user u " +
            "WHERE u.id = :userId")
    List<Long> findGroupIdByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM FavoriteGroup fg WHERE fg.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}