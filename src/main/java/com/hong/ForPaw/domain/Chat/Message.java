package com.hong.ForPaw.domain.Chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
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

    private List<String> imageURLs;

    private LocalDateTime date;

    @Indexed(expireAfterSeconds = 0)
    private Date expireAt;  // TTL 인덱스 필드

    @Builder
    public Message(String id, Long chatRoomId, Long senderId, String nickName, String profileURL, String content, List<String> imageURL, LocalDateTime date, Date expireAt) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.nickName = nickName;
        this.profileURL = profileURL;
        this.content = content;
        this.imageURLs = imageURL;
        this.date = date;
        this.expireAt = expireAt;
    }
}
