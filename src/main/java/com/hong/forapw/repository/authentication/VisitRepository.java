package com.hong.forapw.repository.authentication;

import com.hong.forapw.domain.authentication.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {

    @Query("SELECT v FROM Visit v WHERE v.date >= :date")
    List<Visit> findALlWithinDate(LocalDateTime date);

    @Modifying
    @Query("DELETE FROM Visit v WHERE v.user.id = :userId")
    void deleteByUserId(Long userId);
}
