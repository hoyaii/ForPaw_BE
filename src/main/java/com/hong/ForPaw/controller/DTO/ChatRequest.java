package com.hong.ForPaw.controller.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class ChatRequest {

    public record SendMessageDTO(
            @NotBlank(message = "채팅방 ID를 입력해주세요.")
            Long chatRoomId,
            @NotBlank(message = "내용을 입력해주세요.")
            String content,
            String imageURL) {}

    public record MessageDTO(Long chatRoomId,
                             Long senderId,
                             String senderName,
                             String content,
                             String imageURL,
                             @JsonProperty("date")
                             @JsonSerialize(using = LocalDateTimeSerializer.class)
                             @JsonDeserialize(using = LocalDateTimeDeserializer.class)
                             @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
                             LocalDateTime date) {}

    public record ReadMessageDTO(
            @NotBlank(message = "채팅방 ID를 입력해주세요.")
            Long chatRoomId,
            @NotBlank(message = "메시지 ID를 입력해주세요.")
            Long messageId) {}
}
