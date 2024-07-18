package com.hong.ForPaw.repository.Authentication;

import com.hong.ForPaw.domain.Authentication.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {

    @Query("SELECT v FROM Visit v WHERE v.date >= :date")
    List<Visit> findALlWithinDate(LocalDateTime date);

    void deleteAllByUserId(Long userId);
}
