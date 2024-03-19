package com.hong.ForPaw.controller.DTO;

import java.time.LocalDateTime;

public class MessageRequest {

    public record SendMessageDTO(Long chatRoomId, String content) {}

    public record MessageDTO(Long messageId, Long senderId,String senderName, String content, LocalDateTime time) {}
}
