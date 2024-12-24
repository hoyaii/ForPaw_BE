package com.hong.forapw.domain.chat.entity;

import com.hong.forapw.domain.chat.constant.MessageType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
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

    private String nickName;

    private String profileURL;

    private String content;

    private MessageType messageType;

    private List<String> objectURLs;

    private LinkMetadata metadata;

    @Indexed(expireAfterSeconds = 7890048) // 3개월 후 자동 삭제
    private LocalDateTime date;

    @Builder
    public Message(String id, Long chatRoomId, Long senderId, String nickName, String profileURL, MessageType messageType, String content, List<String> objectURLs, LinkMetadata metadata, LocalDateTime date) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.nickName = nickName;
        this.profileURL = profileURL;
        this.content = content;
        this.messageType = messageType;
        this.objectURLs = objectURLs;
        this.metadata = metadata;
        this.date = date;
    }

    public MessageType getMessageType() {
        return (metadata != null) ? MessageType.LINK : messageType;
    }
}
