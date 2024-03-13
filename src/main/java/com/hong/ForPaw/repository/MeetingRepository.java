package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Group.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    boolean existsById(Long id);

    void deleteAllByGroupId(Long groupId);

    @EntityGraph(attributePaths = {"meetingUsers"})
    Page<Meeting> findByGroupId(Long groupId, Pageable pageable);
}