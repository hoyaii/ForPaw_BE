package com.hong.ForPaw.domain.Chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Document
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Message implements Serializable {

    @Id
    private String id;

    private Long chatRoomId;

    private Long senderId; // 내가 보낸 메시지인지 판별을 위해 도입

    private String senderName;

    private String content;

    private List<String> imageURLs;

    private LocalDateTime date;

    @Builder
    public Message(Long chatRoomId, Long senderId, String senderName, String content, List<String> imageURL,LocalDateTime date) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.imageURLs = imageURL;
        this.date = date;
    }
}
