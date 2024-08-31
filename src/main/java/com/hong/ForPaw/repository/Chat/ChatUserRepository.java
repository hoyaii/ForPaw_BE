package com.hong.ForPaw.repository.Chat;

import com.hong.ForPaw.domain.Chat.ChatRoom;
import com.hong.ForPaw.domain.Chat.ChatUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatUserRepository extends JpaRepository<ChatUser, Long> {

    Optional<ChatUser> findByUserIdAndChatRoomId(Long userId, Long chatRoomId);

    @EntityGraph(attributePaths = {"chatRoom"})
    @Query("SELECT cu FROM ChatUser cu " +
            "JOIN cu.user u " +
            "JOIN cu.chatRoom cr " +
            "WHERE u.id = :userId AND cr.id = :chatRoomId")
    Optional<ChatUser> findByUserIdAndChatRoomIdWithChatRoom(@Param("userId") Long userId, @Param("chatRoomId") Long chatRoomId);

    @Query("SELECT cu FROM ChatUser cu " +
            "JOIN FETCH cu.chatRoom cr " +
            "JOIN FETCH cr.group g " +
            "JOIN cu.user u " +
            "WHERE u.id = :userId")
    List<ChatUser> findByUserIdWithChatRoom(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ChatUser cu WHERE cu.chatRoom.id IN (SELECT cr.id FROM ChatRoom cr WHERE cr.group.id = :groupId)")
    void deleteByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM ChatUser cu WHERE cu.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ChatUser cu WHERE cu.user.id = :userId AND cu.chatRoom.group.id = :groupId")
    void deleteByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
