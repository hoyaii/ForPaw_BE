package com.hong.ForPaw.domain.Chat;

import com.hong.ForPaw.domain.TimeStamp;
import com.hong.ForPaw.domain.User.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Message implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatRoom_id")
    private ChatRoom chatRoom;

    private String senderName;

    private String content;

    private LocalDateTime date;

    @Builder
    public Message(User sender, ChatRoom chatRoom, String senderName, String content, LocalDateTime date) {
        this.sender = sender;
        this.chatRoom = chatRoom;
        this.senderName = senderName;
        this.content = content;
        this.date = date;
    }
}
