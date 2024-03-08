package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Alarm.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    List<Alarm> findByUserId(Long userId);
}
