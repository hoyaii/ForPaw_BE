package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.User.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT u FROM UserStatus u WHERE u.user.role = 'USER'")
    Page<UserStatus> findByAdminRole(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT u FROM UserStatus u WHERE u.user.role = 'USER' or u.user.role = 'ADMIN' AND u.user.removedAt IS NULL")
    Page<UserStatus> findBySuperRole(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT u FROM UserStatus u WHERE u.user.id = :id")
    Optional<UserStatus> findByUserId(@Param("id") Long id);

    void deleteAllByUserId(Long userId);
}
