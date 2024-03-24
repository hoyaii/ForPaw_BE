package com.hong.ForPaw.controller.DTO;

import java.time.LocalDateTime;

public class ChatRequest {

    public record SendMessageDTO(Long chatRoomId, String content) {}

    public record MessageDTO(Long chatRoomId, Long senderId, String senderName, String content, LocalDateTime date) {}

    public record ReadMessageDTO(Long chatRoomId, Long messageId) {}
}
