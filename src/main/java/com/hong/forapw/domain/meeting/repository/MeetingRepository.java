package com.hong.forapw.domain.meeting.repository;

import com.hong.forapw.domain.search.model.GroupMeetingCountDTO;
import com.hong.forapw.domain.meeting.entity.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    Page<Meeting> findByGroupId(Long groupId, Pageable pageable);

    @Query("SELECT m FROM Meeting m WHERE m.meetDate < :date")
    List<Meeting> findByMeetDateBefore(@Param("date") LocalDateTime date);

    @Query("SELECT new com.hong.forapw.controller.dto.query.GroupMeetingCountDTO(g.id, COUNT(m)) " +
            "FROM Meeting m " +
            "JOIN m.group g " +
            "WHERE g.id IN :groupIds " +
            "GROUP BY g.id")
    List<GroupMeetingCountDTO> countMeetingsByGroupIds(@Param("groupIds") List<Long> groupIds);

    @Query("SELECT COUNT(m) > 0 FROM Meeting m " +
            "JOIN m.group g " +
            "WHERE m.name = :name AND g.id = :groupId")
    boolean existsByNameAndGroupId(@Param("name") String name, @Param("groupId") Long groupId);

    @Modifying
    @Query("UPDATE Meeting m SET m.participantNum = m.participantNum - 1 WHERE m.group.id = :groupId AND m.id IN (SELECT mu.meeting.id FROM MeetingUser mu WHERE mu.user.id = :userId) AND m.participantNum > 0")
    void decrementParticipantCountForUserMeetings(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Meeting m WHERE m.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}