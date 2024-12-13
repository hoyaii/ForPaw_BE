package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.ChatRequest;
import com.hong.forapw.controller.dto.ChatResponse;
import com.hong.forapw.domain.chat.ChatUser;
import com.hong.forapw.domain.chat.LinkMetadata;
import com.hong.forapw.domain.chat.Message;
import com.hong.forapw.domain.chat.MessageType;
import com.hong.forapw.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ChatMapper {

    private ChatMapper() {
    }

    public static ChatRequest.MessageDTO toMessageDTO(ChatRequest.SendMessageDTO requestDTO, String senderNickName, String messageId, LinkMetadata metadata, String profileURL, Long senderId) {
        return new ChatRequest.MessageDTO(
                messageId,
                senderNickName,
                profileURL,
                requestDTO.content(),
                (metadata != null) ? MessageType.LINK : requestDTO.messageType(),
                requestDTO.objects(),
                LocalDateTime.now(),
                requestDTO.chatRoomId(),
                senderId,
                metadata);
    }

    public static ChatResponse.MessageDTO toMessageDTO(Message message, Long userId) {
        List<ChatResponse.ChatObjectDTO> imageDTOs = message.getObjectURLs().stream()
                .map(ChatResponse.ChatObjectDTO::new)
                .toList();

        return new ChatResponse.MessageDTO(
                message.getId(),
                message.getNickName(),
                message.getProfileURL(),
                message.getContent(),
                message.getMessageType(),
                imageDTOs,
                message.getMetadata(),
                message.getDate(),
                message.getSenderId().equals(userId));
    }

    public static ChatResponse.RoomDTO toRoomDTO(ChatUser chatUser, String lastMessageContent, LocalDateTime lastMessageDate, long offset) {
        return new ChatResponse.RoomDTO(
                chatUser.getChatRoomId(),
                chatUser.getRoomName(),
                lastMessageContent,
                lastMessageDate,
                offset,
                chatUser.getGroupProfileURL());
    }

    public static ChatResponse.ChatUserDTO toChatUserDTO(User user) {
        return new ChatResponse.ChatUserDTO(
                user.getId(),
                user.getNickname(),
                user.getProfileURL());
    }

    public static ChatResponse.ImageObjectDTO toImageObjectDTO(Message message) {
        List<ChatResponse.ChatObjectDTO> chatObjectDTOs = message.getObjectURLs().stream()
                .map(ChatResponse.ChatObjectDTO::new)
                .toList();

        return new ChatResponse.ImageObjectDTO(
                message.getId(),
                message.getNickName(),
                message.getProfileURL(),
                chatObjectDTOs,
                message.getDate());
    }

    public static ChatResponse.FileObjectDTO toFileObjectDTO(Message message) {
        List<ChatResponse.ChatObjectDTO> chatObjectDTOS = message.getObjectURLs().stream()
                .map(ChatResponse.ChatObjectDTO::new)
                .toList();

        return new ChatResponse.FileObjectDTO(
                message.getId(),
                message.getContent(),
                chatObjectDTOS,
                message.getDate());
    }

    public static ChatResponse.LinkObjectDTO toLinkObjectDTO(Message message) {
        LinkMetadata metadata = message.getMetadata();
        String title = metadata != null ? metadata.getTitle() : null;
        String description = metadata != null ? metadata.getDescription() : null;
        String image = metadata != null ? metadata.getImage() : null;
        String ogUrl = metadata != null ? metadata.getOgUrl() : null;

        return new ChatResponse.LinkObjectDTO(message.getId(), title, description, image, ogUrl, message.getDate());
    }
}
