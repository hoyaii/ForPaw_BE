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

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    boolean existsById(Long id);

    void deleteAllByGroupId(Long groupId);

    Page<Meeting> findByGroupId(Long groupId, Pageable pageable);

    @Modifying
    @Query("UPDATE Meeting m SET m.participantNum = m.participantNum + 1 WHERE m.id = :meetingId")
    void incrementParticipantNum(@Param("meetingId") Long meetingId);

    @Modifying
    @Query("UPDATE Meeting m SET m.participantNum = m.participantNum - 1 WHERE m.id = :groupId AND m.participantNum > 0")
    void decrementParticipantNum(@Param("meetingId") Long meetingId);
}