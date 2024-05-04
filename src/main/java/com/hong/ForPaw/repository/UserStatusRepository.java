package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.User.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {
}
