package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.Chat.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public class ChatResponse {

    public record SendMessageDTO(String messageId) {}

    public record FindMessageListInRoomDTO(String chatRoomName, String lastMessageId, String myNickName, List<MessageDTO> messages) {}

    public record MessageDTO(String messageId,
                             String nickName,
                             String profileURL,
                             String content,
                             MessageType messageType,
                             List<ChatObjectDTO> objects,
                             LocalDateTime date,
                             boolean isMine) {}

    public record FindChatRoomDrawerDTO(List<ImageObjectDTO> images, List<ChatUserDTO> users) {}

    public record ChatObjectDTO(String objectURL) {}

    public record ImageObjectDTO(String messageId,
                                 String nickName,
                                 String profileURL,
                                 List<ChatObjectDTO> objects,
                                 LocalDateTime date){}

    public record FileObjectDTO(String messageId,
                                 String fileName,
                                 List<ChatObjectDTO> objects,
                                 LocalDateTime date){}

    public record LinkObjectDTO(String messageId,
                                String linkURL,
                                String title,
                                String description,
                                String image,
                                String ogUrl,
                                LocalDateTime date){}

    public record ChatUserDTO(Long userId,
                              String nickName,
                              String profileURL) {}

    public record FindChatRoomListDTO(List<ChatResponse.RoomDTO> rooms) {}

    public record RoomDTO(Long chatRoomId,
                          String name,
                          String lastMessageContent,
                          LocalDateTime lastMessageTime,
                          Long offset) {}

    public record FindChatRoomImageList(List<ImageObjectDTO> images) {}

    public record FindChatRoomFileListDTO(List<FileObjectDTO> files) {}

    public record FindChatRoomLinkListDTO(List<LinkObjectDTO> links) {}

    public record ReadMessageDTO(String id) {}
}
