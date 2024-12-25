package com.hong.forapw.domain.user.repository;

import com.hong.forapw.domain.user.entity.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT u FROM UserStatus u WHERE u.user.id = :id")
    Optional<UserStatus> findByUserId(@Param("id") Long id);
}
