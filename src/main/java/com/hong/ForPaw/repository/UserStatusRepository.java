package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.domain.User.UserRole;
import com.hong.ForPaw.domain.User.UserStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    UserStatus findByUserIdOne(@Param("id") Long id);
}
