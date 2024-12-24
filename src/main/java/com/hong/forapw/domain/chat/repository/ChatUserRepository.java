package com.hong.forapw.domain.chat.repository;

import com.hong.forapw.domain.chat.entity.ChatUser;
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

    @Query("SELECT cu FROM ChatUser cu WHERE cu.user.id = :userId AND cu.chatRoom.id = :chatRoomId")
    Optional<ChatUser> findByUserIdAndChatRoomId(@Param("userId") Long userId, @Param("chatRoomId") Long chatRoomId);

    @Query("SELECT CASE WHEN COUNT(cu) > 0 THEN TRUE ELSE FALSE END " +
            "FROM ChatUser cu WHERE cu.user.id = :userId AND cu.chatRoom.id = :chatRoomId")
    boolean existsByUserIdAndChatRoomId(@Param("userId") Long userId, @Param("chatRoomId") Long chatRoomId);

    @EntityGraph(attributePaths = {"chatRoom"})
    @Query("SELECT cu FROM ChatUser cu " +
            "JOIN cu.user u " +
            "JOIN cu.chatRoom cr " +
            "WHERE u.id = :userId AND cr.id = :chatRoomId")
    Optional<ChatUser> findByUserIdAndChatRoomIdWithChatRoom(@Param("userId") Long userId, @Param("chatRoomId") Long chatRoomId);

    @EntityGraph(attributePaths = {"chatRoom.group"})
    @Query("SELECT cu FROM ChatUser cu " +
            "JOIN cu.user u " +
            "WHERE u.id = :userId")
    List<ChatUser> findAllByUserIdWithChatRoomAndGroup(@Param("userId") Long userId);

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
