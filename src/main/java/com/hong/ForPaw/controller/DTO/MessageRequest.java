package com.hong.ForPaw.controller.DTO;

import java.time.LocalDateTime;

public class MessageRequest {

    public record MessageDTO(Long senderId, Long chatRoomId, String sender, String content, LocalDateTime time) {}
}
