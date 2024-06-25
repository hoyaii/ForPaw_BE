package com.hong.ForPaw.domain.Chat;

import com.hong.ForPaw.domain.TimeStamp;
import com.hong.ForPaw.domain.User.User;
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
    private String lastMessageId;

    @Column
    private Long lastMessageIdx = 0L;

    @Builder
    public ChatUser(User user, ChatRoom chatRoom) {
        this.user = user;
        this.chatRoom = chatRoom;
    }

    public void updateLastMessage(String lastMessageId, Long lastMessageIdx){
        this.lastMessageId = lastMessageId;
        this.lastMessageIdx = lastMessageIdx;
    }
}
