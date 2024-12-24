package com.hong.forapw.domain.chat.repository;

import com.hong.forapw.domain.chat.entity.ChatRoom;
import com.hong.forapw.domain.group.constant.GroupRole;
import com.hong.forapw.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByGroupId(Long groupId);

    @Query("SELECT gu.user FROM GroupUser gu WHERE gu.group.id = (SELECT cr.group.id FROM ChatRoom cr WHERE cr.id = :chatRoomId) AND gu.groupRole <> :excludedRole")
    List<User> findUsersByChatRoomIdExcludingRole(@Param("chatRoomId") Long chatRoomId, @Param("excludedRole") GroupRole excludedRole);
}