package com.hong.ForPaw.controller.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRequest {

    public record SendMessageDTO(
            @NotNull(message = "채팅방 ID를 입력해주세요.")
            Long chatRoomId,
            @NotBlank(message = "내용을 입력해주세요.")
            String content,
            List<ChatImageDTO> images,
            LocalDateTime sendDate) {}

    public record ChatImageDTO(String imageURL){}

    public record MessageDTO(String id,
                             Long chatRoomId,
                             Long senderId,
                             String senderName,
                             String content,
                             List<ChatImageDTO> images,
                             @JsonSerialize(using = LocalDateTimeSerializer.class)
                             @JsonDeserialize(using = LocalDateTimeDeserializer.class)
                             @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
                             LocalDateTime date) {}

    public record ReadMessageDTO(
            @NotNull(message = "채팅방 ID를 입력해주세요.")
            Long chatRoomId,
            @NotNull(message = "메시지 ID를 입력해주세요.")
            String messageId) {}
}
