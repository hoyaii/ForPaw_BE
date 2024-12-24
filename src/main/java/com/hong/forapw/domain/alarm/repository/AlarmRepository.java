package com.hong.forapw.domain.alarm.repository;

import com.hong.forapw.domain.alarm.entity.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    @Query("SELECT a FROM Alarm a WHERE a.receiver.id = :receiverId")
    List<Alarm> findByReceiverId(@Param("receiverId") Long receiverId);

    @Query("SELECT a FROM Alarm a WHERE a.receiver.id = :userId AND a.isRead = false")
    List<Alarm> findByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Alarm a WHERE a.receiver.id = :userId")
    void deleteByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM Alarm a WHERE a.readDate <= :cutoffDate AND a.isRead = true")
    void deleteReadAlarmBefore(LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM Alarm a WHERE a.createdDate <= :cutoffDate AND a.isRead = false")
    void deleteNotReadAlarmBefore(LocalDateTime cutoffDate);
}