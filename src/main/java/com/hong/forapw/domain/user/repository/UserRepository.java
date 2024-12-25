package com.hong.forapw.domain.user.repository;

import com.hong.forapw.domain.region.constant.District;
import com.hong.forapw.domain.region.constant.Province;
import com.hong.forapw.domain.user.constant.AuthProvider;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.constant.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.removedAt IS NULL")
    List<User> findAllNonWithdrawn();

    @Query("SELECT u.nickname FROM User u WHERE u.id = :id AND u.removedAt IS NULL")
    Optional<String> findNickname(@Param("id") Long id);

    @Query("SELECT u.profileURL FROM User u WHERE u.id = :id AND u.removedAt IS NULL")
    Optional<String> findProfileURL(@Param("id") Long id);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.removedAt IS NULL")
    Optional<User> findNonWithdrawnById(@Param("id") Long id);

    @Query("SELECT u.role FROM User u WHERE u.id = :id AND u.removedAt IS NULL")
    Optional<UserRole> findRoleById(@Param("id") Long id);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status.isActive = true AND u.removedAt IS NULL")
    Long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.status.isActive = false AND u.removedAt IS NULL")
    Long countNotActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdDate >= :date AND u.removedAt IS NULL")
    Long countAllUsersCreatedAfter(LocalDateTime date);

    @Query("SELECT u.district FROM User u WHERE u.id = :id AND u.removedAt IS NULL")
    Optional<District> findDistrictById(@Param("id") Long id);

    @Query("SELECT u.province FROM User u WHERE u.id = :id AND u.removedAt IS NULL")
    Optional<Province> findProvinceById(@Param("id") Long id);

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithRemoved(@Param("id") Long id);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.removedAt IS NULL")
    Optional<User> findByEmail(@Param("email") String email);

    @EntityGraph(attributePaths = {"status"})
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithRemoved(@Param("email") String email);

    @Query("SELECT u.profileURL FROM User u WHERE u.id = :userId AND u.removedAt IS NULL")
    Optional<String> findProfileById(@Param("userId") Long userId);

    @Query("SELECT u.authProvider FROM User u WHERE u.email = :email AND u.removedAt IS NULL")
    Optional<AuthProvider> findAuthProviderByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.removedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.nickname = :nickName")
    boolean existsByNicknameWithRemoved(@Param("nickName") String nickName);

    @Modifying
    @Query("DELETE FROM User u WHERE u.removedAt <= :cutoffDate AND u.removedAt IS NOT NULL")
    void deleteBySoftDeletedBefore(LocalDateTime cutoffDate);

    @Modifying
    @Query("UPDATE User u SET u.removedAt = NOW() WHERE u.id= :id")
    void markAsRemovedById(@Param("id") Long id);
}