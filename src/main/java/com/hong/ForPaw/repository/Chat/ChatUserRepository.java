package com.hong.ForPaw.repository.Chat;

import com.hong.ForPaw.domain.Chat.ChatRoom;
import com.hong.ForPaw.domain.Chat.ChatUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatUserRepository extends JpaRepository<ChatUser, Long> {

    Optional<ChatUser> findByUserIdAndChatRoom(Long userId, ChatRoom chatRoom);

    Optional<ChatUser> findByUserIdAndChatRoomId(Long userId, Long chatRoomId);

    @EntityGraph(attributePaths = {"chatRoom"})
    @Query("SELECT cu FROM ChatUser cu WHERE cu.user.id = :userId")
    List<ChatUser> findByUserIdWithChatRoom(Long userId);

    @Query("SELECT cu.chatRoom FROM ChatUser cu WHERE cu.user.id = :userId")
    List<ChatRoom> findChatRoomsByUserId(@Param("userId") Long userId);
}
