package com.hong.forapw.service;

import com.hong.forapw.controller.dto.ChatRequest;
import com.hong.forapw.controller.dto.ChatResponse;
import com.hong.forapw.controller.dto.MessageDetailDTO;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.core.utils.MetaDataUtils;
import com.hong.forapw.core.utils.mapper.ChatMapper;
import com.hong.forapw.domain.chat.ChatUser;
import com.hong.forapw.domain.chat.LinkMetadata;
import com.hong.forapw.domain.chat.Message;
import com.hong.forapw.domain.chat.MessageType;
import com.hong.forapw.domain.group.GroupRole;
import com.hong.forapw.repository.chat.ChatRoomRepository;
import com.hong.forapw.repository.chat.ChatUserRepository;
import com.hong.forapw.repository.chat.MessageRepository;
import com.hong.forapw.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hong.forapw.core.utils.mapper.ChatMapper.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final MessageRepository messageRepository;
    private final ChatUserRepository chatUserRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final BrokerService brokerService;

    private static final String SORT_BY_MESSAGE_DATE = "date";
    private static final String URL_REGEX = "(https?://[\\w\\-\\._~:/?#\\[\\]@!$&'()*+,;=%]+)";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
    private static final Pageable DEFAULT_IMAGE_PAGEABLE = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, SORT_BY_MESSAGE_DATE));

    @Transactional
    public ChatResponse.SendMessageDTO sendMessage(ChatRequest.SendMessageDTO requestDTO, Long senderId, String senderNickName) {
        validateChatAuthorization(senderId, requestDTO.chatRoomId());

        String messageId = UUID.randomUUID().toString();
        LinkMetadata metadata = extractMetadataIfApplicable(requestDTO);
        String senderProfileURL = getUserProfileURL(senderId);

        ChatRequest.MessageDTO messageDTO = toMessageDTO(requestDTO, senderNickName, messageId, metadata, senderProfileURL, senderId);
        publishMessageToBroker(requestDTO.chatRoomId(), messageDTO);

        return new ChatResponse.SendMessageDTO(messageId);
    }

    public ChatResponse.FindChatRoomsDTO findChatRooms(Long userId) {
        List<ChatUser> chatUsers = chatUserRepository.findAllByUserIdWithChatRoomAndGroup(userId);

        List<ChatResponse.RoomDTO> roomDTOS = chatUsers.stream()
                .map(this::buildRoomDTO)
                .collect(Collectors.toList());

        return new ChatResponse.FindChatRoomsDTO(roomDTOS);
    }

    @Transactional
    public ChatResponse.FindMessagesInRoomDTO findMessagesInRoom(Long chatRoomId, Long userId, Pageable pageable) {
        String nickName = userRepository.findNickname(userId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        ChatUser chatUser = chatUserRepository.findByUserIdAndChatRoomIdWithChatRoom(userId, chatRoomId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_FORBIDDEN)
        );

        List<Message> messages = messageRepository.findByChatRoomId(chatRoomId, pageable).getContent();
        List<ChatResponse.MessageDTO> messageDTOs = convertToMessageDTOs(messages, userId);
        Collections.reverse(messageDTOs);

        updateLastReadMessage(chatUser, messageDTOs, chatRoomId);
        return new ChatResponse.FindMessagesInRoomDTO(chatUser.getRoomName(), chatUser.getLastReadMessageId(), nickName, messageDTOs);
    }

    public ChatResponse.FindChatRoomDrawerDTO findChatRoomDrawer(Long chatRoomId, Long userId) {
        validateChatAuthorization(userId, chatRoomId);

        List<ChatResponse.ChatUserDTO> chatUserDTOs = chatRoomRepository.findUsersByChatRoomIdExcludingRole(chatRoomId, GroupRole.TEMP).stream()
                .map(ChatMapper::toChatUserDTO)
                .toList();

        List<ChatResponse.ImageObjectDTO> imageObjectDTOS = findImageObjects(chatRoomId, userId, DEFAULT_IMAGE_PAGEABLE).images();
        return new ChatResponse.FindChatRoomDrawerDTO(imageObjectDTOS, chatUserDTOs);
    }

    public ChatResponse.FindImageObjectsDTO findImageObjects(Long chatRoomId, Long userId, Pageable pageable) {
        validateChatAuthorization(userId, chatRoomId);

        Page<Message> imageMessages = messageRepository.findByChatRoomIdAndMessageType(chatRoomId, MessageType.IMAGE, pageable);
        List<ChatResponse.ImageObjectDTO> imageObjectDTOs = imageMessages.getContent().stream()
                .map(ChatMapper::toImageObjectDTO)
                .toList();

        return new ChatResponse.FindImageObjectsDTO(imageObjectDTOs, imageMessages.isLast());
    }

    public ChatResponse.FindFileObjects findFileObjects(Long chatRoomId, Long userId, Pageable pageable) {
        validateChatAuthorization(userId, chatRoomId);

        Page<Message> fileMessages = messageRepository.findByChatRoomIdAndMessageType(chatRoomId, MessageType.FILE, pageable);
        List<ChatResponse.FileObjectDTO> fileObjectDTOs = fileMessages.getContent().stream()
                .map(ChatMapper::toFileObjectDTO)
                .toList();

        return new ChatResponse.FindFileObjects(fileObjectDTOs, fileMessages.isLast());
    }

    public ChatResponse.FindLinkObjects findLinkObjects(Long chatRoomId, Long userId, Pageable pageable) {
        validateChatAuthorization(userId, chatRoomId);

        Page<Message> linkMessages = messageRepository.findByChatRoomIdAndMetadataOgUrlIsNotNull(chatRoomId, pageable);
        List<ChatResponse.LinkObjectDTO> linkObjectDTOS = linkMessages.getContent().stream()
                .map(ChatMapper::toLinkObjectDTO)
                .toList();

        return new ChatResponse.FindLinkObjects(linkObjectDTOS, linkMessages.isLast());
    }

    @Transactional
    public ChatResponse.ReadMessageDTO readMessage(String messageId) {
        Message message = messageRepository.findById(messageId).orElseThrow(
                () -> new CustomException(ExceptionCode.MESSAGE_NOT_FOUND)
        );

        ChatUser chatUser = chatUserRepository.findByUserIdAndChatRoomId(message.getSenderId(), message.getChatRoomId())
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_FORBIDDEN));

        chatUser.updateLastMessage(message.getId(), chatUser.getLastReadMessageIndex() + 1);
        return new ChatResponse.ReadMessageDTO(messageId);
    }

    private void validateChatAuthorization(Long senderId, Long chatRoomId) {
        boolean isMemberOfChatRoom = chatUserRepository.existsByUserIdAndChatRoomId(senderId, chatRoomId);
        if (!isMemberOfChatRoom) {
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private String getUserProfileURL(Long senderId) {
        return userRepository.findProfileURL(senderId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );
    }

    private LinkMetadata extractMetadataIfApplicable(ChatRequest.SendMessageDTO requestDTO) {
        if (requestDTO.messageType() == MessageType.TEXT) {
            String metadataURL = extractFirstURL(requestDTO.content());
            return (metadataURL != null) ? MetaDataUtils.fetchMetadata(metadataURL) : null;
        }
        return null;
    }

    private String extractFirstURL(String content) {
        Matcher matcher = URL_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private void publishMessageToBroker(Long chatRoomId, ChatRequest.MessageDTO messageDTO) {
        CompletableFuture.runAsync(() -> brokerService.produceChatToRoom(chatRoomId, messageDTO));
    }

    private ChatResponse.RoomDTO buildRoomDTO(ChatUser chatUser) {
        String lastMessageId = chatUser.getLastReadMessageId();
        MessageDetailDTO lastMessageDetails = fetchLastMessageContentAndDate(lastMessageId);

        Long unreadMessageIndex = chatUser.getLastReadMessageIndex();
        long unreadMessageOffset = calculateUnreadMessageOffset(chatUser.getChatRoom().getId(), unreadMessageIndex);

        return toRoomDTO(chatUser, lastMessageDetails.content(), lastMessageDetails.date(), unreadMessageOffset);
    }

    private MessageDetailDTO fetchLastMessageContentAndDate(String lastMessageId) {
        if (lastMessageId == null) {
            return new MessageDetailDTO(null, null);
        }

        return messageRepository.findById(lastMessageId)
                .map(message -> new MessageDetailDTO(message.getContent(), message.getDate()))
                .orElse(new MessageDetailDTO(null, null));
    }

    private long calculateUnreadMessageOffset(Long chatRoomId, Long lastReadMessageIdx) {
        long totalMessages = messageRepository.countByChatRoomId(chatRoomId);
        long totalPages = totalMessages != 0L ? (totalMessages / 50) : 0L;

        if (lastReadMessageIdx == null || lastReadMessageIdx == 0L) {
            return totalPages;
        }

        long lastReadPage = lastReadMessageIdx / 50;
        return totalPages - lastReadPage;
    }

    private List<ChatResponse.MessageDTO> convertToMessageDTOs(List<Message> messages, Long userId) {
        return messages.stream()
                .map(message -> toMessageDTO(message, userId))
                .toList();
    }

    private void updateLastReadMessage(ChatUser chatUser, List<ChatResponse.MessageDTO> messageDTOs, Long chatRoomId) {
        if (!messageDTOs.isEmpty()) {
            ChatResponse.MessageDTO lastMessage = messageDTOs.get(messageDTOs.size() - 1);
            long totalMessages = messageRepository.countByChatRoomId(chatRoomId);
            chatUser.updateLastMessage(lastMessage.messageId(), totalMessages - 1);
        }
    }
}