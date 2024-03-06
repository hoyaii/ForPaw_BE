package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Group.GroupUser;
import com.hong.ForPaw.domain.Group.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface GroupUserRepository extends JpaRepository<GroupUser, Long> {

    Optional<GroupUser> findByGroupIdAndUserId(Long groupId, Long userId);

    Page<GroupUser> findByUserId(Long userId, Pageable pageable);

    void deleteByGroupIdAndUserId(Long groupId, Long userId);

    void deleteAllByGroupId(Long groupId);

    @Modifying
    @Query("UPDATE GroupUser gu SET gu.role = :role WHERE gu.group.id = :groupId AND gu.user.id = :userId")
    void updateRole(@Param("role") Role role, @Param("groupId") Long groupId, @Param("userId") Long userId);
}
