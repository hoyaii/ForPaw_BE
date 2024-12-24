package com.hong.forapw.domain.group.repository;

import com.hong.forapw.domain.group.entity.Group;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.group.entity.GroupUser;
import com.hong.forapw.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface GroupUserRepository extends JpaRepository<GroupUser, Long> {

    @Query("SELECT gu FROM GroupUser gu WHERE gu.group.id = :groupId AND gu.user.id = :userId")
    Optional<GroupUser> findByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    List<GroupUser> findAllByUser(User user);

    @EntityGraph(attributePaths = {"group"})
    @Query("SELECT gu FROM GroupUser gu " +
            "JOIN gu.user u " +
            "WHERE u.id = :userId")
    List<GroupUser> findByUserIdWithGroup(Long userId);

    @Query("SELECT u FROM GroupUser gu " +
            "JOIN gu.user u " +
            "WHERE gu.group.id = :groupId AND u.id NOT IN (:myId)")
    List<User> findUserByGroupIdWithoutMe(@Param("groupId") Long groupId, @Param("myId") Long myId);

    @EntityGraph(attributePaths = {"group"})
    @Query("SELECT gu FROM GroupUser gu " +
            "JOIN gu.group g " +
            "WHERE g.id = :groupId")
    List<GroupUser> findByGroupIdWithGroup(@Param("groupId") Long groupId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT gu FROM GroupUser gu " +
            "JOIN gu.group g " +
            "WHERE g.id = :groupId AND gu.groupRole = :groupRole")
    List<GroupUser> findByGroupRole(@Param("groupId") Long groupId, @Param("groupRole") GroupRole groupRole);

    @Query("SELECT g FROM GroupUser gu " +
            "JOIN gu.group g " +
            "JOIN gu.user u WHERE u.id = :userId")
    List<Group> findGroupByUserId(@Param("userId") Long userId);

    @Query("SELECT g FROM GroupUser gu " +
            "JOIN gu.group g " +
            "JOIN gu.user u " +
            "WHERE u.id = :userId AND gu.groupRole != 'TEMP'")
    Page<Group> findGroupByUserId(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT gu FROM GroupUser gu " +
            "JOIN gu.group g " +
            "WHERE g.id = :groupId " +
            "ORDER BY gu.createdDate ASC")
    List<GroupUser> findByGroupIdWithUserInAsc(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(gu) > 0 FROM GroupUser gu " +
            "JOIN gu.group g " +
            "JOIN gu.user u " +
            "WHERE g.id = :groupId AND u.id = :userId")
    boolean existsByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE GroupUser gu SET gu.groupRole = :groupRole WHERE gu.group.id = :groupId AND gu.user.id = :userId")
    void updateRole(@Param("groupRole") GroupRole groupRole, @Param("groupId") Long groupId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM GroupUser gu WHERE gu.group.id = :groupId AND gu.user.id = :userId")
    void deleteByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM GroupUser gu WHERE gu.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}