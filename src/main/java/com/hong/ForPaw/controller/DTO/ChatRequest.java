package com.hong.ForPaw.controller.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ChatRequest {

    public record SendMessageDTO(
            @NotNull(message = "채팅방 ID를 입력해주세요.")
            Long chatRoomId,
            @NotBlank(message = "내용을 입력해주세요.")
            String content,
            List<ChatImageDTO> images) {}

    public record ChatImageDTO(String imageURL){}

    public record MessageDTO(Long chatRoomId,
                             Long senderId,
                             String senderName,
                             String content,
                             List<ChatImageDTO> imageURLs) {}

    public record ReadMessageDTO(
            @NotNull(message = "채팅방 ID를 입력해주세요.")
            Long chatRoomId,
            @NotNull(message = "메시지 ID를 입력해주세요.")
            String messageId) {}
}
