package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Alarm.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    @Query("SELECT a FROM Alarm a WHERE a.receiver.id = :userId AND a.isRead = false")
    List<Alarm> findByUserId(@Param("userId") Long userId);
}