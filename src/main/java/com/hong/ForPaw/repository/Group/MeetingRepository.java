package com.hong.ForPaw.repository.Group;

import com.hong.ForPaw.domain.Group.Meeting;
import com.hong.ForPaw.domain.Post.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    boolean existsById(Long id);

    Page<Meeting> findByGroupId(Long groupId, Pageable pageable);

    @Modifying
    @Query("UPDATE Meeting m SET m.participantNum = m.participantNum + 1 WHERE m.id = :meetingId")
    void incrementParticipantNum(@Param("meetingId") Long meetingId);

    @Modifying
    @Query("UPDATE Meeting m SET m.participantNum = m.participantNum - 1 WHERE m.id = :meetingId AND m.participantNum > 0")
    void decrementParticipantNum(@Param("meetingId") Long meetingId);

    @Modifying
    @Query("UPDATE Meeting m SET m.participantNum = m.participantNum - 1 WHERE m.group.id = :groupId AND m.id IN (SELECT mu.meeting.id FROM MeetingUser mu WHERE mu.user.id = :userId) AND m.participantNum > 0")
    void decrementParticipantNum(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT COUNT(m) > 0 FROM Meeting m WHERE m.name = :name AND m.group.id = :groupId")
    boolean existsByNameAndGroupId(@Param("name") String name, @Param("groupId") Long groupId);

    @Query("SELECT m FROM Meeting m WHERE m.meetDate < :date")
    List<Meeting> findAllByMeetDateBefore(@Param("date") LocalDateTime date);

    @Modifying
    @Query("DELETE FROM Meeting m WHERE m.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}