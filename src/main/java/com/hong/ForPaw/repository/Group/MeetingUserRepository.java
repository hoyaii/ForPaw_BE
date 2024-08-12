package com.hong.ForPaw.repository.Group;

import com.hong.ForPaw.controller.DTO.Query.MeetingUserProfileDTO;
import com.hong.ForPaw.domain.Group.MeetingUser;
import com.hong.ForPaw.domain.User.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingUserRepository extends JpaRepository<MeetingUser, Long> {

    @Query("SELECT mu.user FROM MeetingUser mu WHERE mu.meeting.id = :meetingId")
    List<User> findUserByMeetingId(@Param("meetingId") Long meetingId);

    @EntityGraph(attributePaths = {"meeting"})
    @Query("SELECT gu FROM GroupUser gu WHERE gu.user.id = :userId")
    List<MeetingUser> findByUserIdWithMeeting(Long userId);

    @Query("SELECT DISTINCT new com.hong.ForPaw.controller.DTO.Query.MeetingUserProfileDTO(m.id, u.profileURL)" +
            "FROM MeetingUser mu " +
            "JOIN mu.meeting m " +
            "JOIN mu.user u " +
            "WHERE mu.meeting.group.id = :groupId")
    List<MeetingUserProfileDTO> findMeetingUsersByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(m) > 0 FROM MeetingUser m WHERE m.meeting.id = :meetingId AND m.user.id = :userId")
    boolean existsByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    void deleteByMeetingIdAndUserId(Long meetingId, Long userId);

    void deleteAllByMeetingId(Long meetingId);

    @Modifying
    @Query("DELETE FROM MeetingUser mu WHERE mu.meeting.id IN (SELECT m.id FROM Meeting m WHERE m.group.id = :groupId)")
    void deleteByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM MeetingUser mu WHERE mu.user.id = :userId AND mu.meeting.id IN (SELECT m.id FROM Meeting m WHERE m.group.id = :groupId)")
    void deleteByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
