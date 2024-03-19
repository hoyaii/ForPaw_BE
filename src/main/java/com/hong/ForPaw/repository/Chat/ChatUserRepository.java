package com.hong.ForPaw.repository.Chat;

import com.hong.ForPaw.domain.Chat.ChatRoom;
import com.hong.ForPaw.domain.Chat.ChatUser;
import com.hong.ForPaw.domain.User.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatUserRepository extends JpaRepository<ChatUser, Long> {

    ChatUser findByUserAndChatRoom(User user, ChatRoom chatRoom);

    @EntityGraph(attributePaths = {"chatRoom"})
    List<ChatUser> findByUserId(Long userId);

    @Query("SELECT cu.chatRoom FROM ChatUser cu WHERE cu.user.id = :userId")
    List<ChatRoom> findChatRoomsByUserId(@Param("userId") Long userId);
}
