package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Group.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    boolean existsById(Long id);
}