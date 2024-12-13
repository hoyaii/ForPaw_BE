package com.hong.forapw.domain.chat;

import com.hong.forapw.domain.TimeStamp;
import com.hong.forapw.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chatUser_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ChatUser extends TimeStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatRoom_id")
    private ChatRoom chatRoom;

    @Column
    private String lastReadMessageId;

    @Column
    private Long lastReadMessageIndex = 0L;

    @Builder
    public ChatUser(User user, ChatRoom chatRoom) {
        this.user = user;
        this.chatRoom = chatRoom;
    }

    public void updateLastMessage(String lastReadMessageId, Long lastReadMessageIndex) {
        this.lastReadMessageId = lastReadMessageId;
        this.lastReadMessageIndex = lastReadMessageIndex;
    }

    public Long getChatRoomId() {
        return chatRoom.getId();
    }

    public String getRoomName() {
        return chatRoom.getName();
    }

    public String getGroupProfileURL() {
        return chatRoom.getGroup().getProfileURL();
    }
}
