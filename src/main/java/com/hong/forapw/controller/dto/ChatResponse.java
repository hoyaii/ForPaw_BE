package com.hong.forapw.controller.dto;

import com.hong.forapw.domain.chat.LinkMetadata;
import com.hong.forapw.domain.chat.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public class ChatResponse {

    public record SendMessageDTO(String messageId) {
    }

    public record FindMessageListInRoomDTO(String chatRoomName, String lastMessageId, String myNickName,
                                           List<MessageDTO> messages) {
    }

    public record MessageDTO(String messageId,
                             String nickName,
                             String profileURL,
                             String content,
                             MessageType messageType,
                             List<ChatObjectDTO> objects,
                             LinkMetadata linkMetadata,
                             LocalDateTime date,
                             boolean isMine) {
    }

    public record FindChatRoomDrawerDTO(List<ImageObjectDTO> images, List<ChatUserDTO> users) {
    }

    public record ChatObjectDTO(String objectURL) {
    }

    public record ImageObjectDTO(String messageId,
                                 String nickName,
                                 String profileURL,
                                 List<ChatObjectDTO> objects,
                                 LocalDateTime date) {
    }

    public record FileObjectDTO(String messageId,
                                String fileName,
                                List<ChatObjectDTO> objects,
                                LocalDateTime date) {
    }

    public record LinkObjectDTO(String messageId,
                                String title,
                                String description,
                                String image,
                                String ogUrl,
                                LocalDateTime date) {
    }

    public record ChatUserDTO(Long userId,
                              String nickName,
                              String profileURL) {
    }

    public record FindChatRoomListDTO(List<ChatResponse.RoomDTO> rooms) {
    }

    public record RoomDTO(Long chatRoomId,
                          String name,
                          String lastMessageContent,
                          LocalDateTime lastMessageTime,
                          Long offset,
                          String profileURL) {
    }

    public record FindImageObjectListDTO(List<ImageObjectDTO> images, boolean isLastPage) {
    }

    public record FindFileObjectList(List<FileObjectDTO> files, boolean isLastPage) {
    }

    public record FindLinkObjectList(List<LinkObjectDTO> links, boolean isLastPage) {
    }

    public record ReadMessageDTO(String id) {
    }
}
