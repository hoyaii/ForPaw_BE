package com.hong.ForPaw.repository.Chat;

import com.hong.ForPaw.domain.Chat.ChatRoom;
import com.hong.ForPaw.domain.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByGroupId(Long groupId);

    @Query("SELECT gu.user FROM GroupUser gu WHERE gu.group.id = (SELECT cr.group.id FROM ChatRoom cr WHERE cr.id = :chatRoomId) AND gu.groupRole <> com.hong.ForPaw.domain.Group.GroupRole.TEMP")
    List<User> findUsersByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}