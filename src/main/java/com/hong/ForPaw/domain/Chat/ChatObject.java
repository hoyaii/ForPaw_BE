package com.hong.ForPaw.domain.Chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chatObject_tb")
@NoArgsConstructor(access =  AccessLevel.PROTECTED)
@Getter
public class ChatObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatRoom_id")
    private ChatRoom chatRoom;

    @Column
    private String objectURL;

    @Builder
    public ChatObject(Long id, ChatRoom chatRoom, String objectURL) {
        this.id = id;
        this.chatRoom = chatRoom;
        this.objectURL = objectURL;
    }

    public void updateChatRoom(ChatRoom chatRoom){
        this.chatRoom = chatRoom;
    }
}
