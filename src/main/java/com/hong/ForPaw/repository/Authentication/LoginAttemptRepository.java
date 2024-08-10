package com.hong.ForPaw.repository.Authentication;

import com.hong.ForPaw.domain.Authentication.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.user.id = :userId")
    void deleteByUserId(Long userId);
}
