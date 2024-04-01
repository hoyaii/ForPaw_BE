package com.hong.ForPaw.domain.Chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chatImage_tb")
@NoArgsConstructor(access =  AccessLevel.PROTECTED)
@Getter
public class ChatImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatRoom_id")
    private ChatRoom chatRoom;

    @Column
    private String imageURL;

    @Builder
    public ChatImage(Long id, ChatRoom chatRoom, String imageURL) {
        this.id = id;
        this.chatRoom = chatRoom;
        this.imageURL = imageURL;
    }

    public void updateChatRoom(ChatRoom chatRoom){
        this.chatRoom = chatRoom;
    }
}
