package com.hong.forapw.service;

import com.hong.forapw.controller.dto.ChatRequest;
import com.hong.forapw.controller.dto.ChatResponse;
import com.hong.forapw.controller.dto.MessageDetailDTO;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.core.utils.MetaDataUtils;
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

import static com.hong.forapw.core.utils.mapper.ChatMapper.toMessageDTO;
import static com.hong.forapw.core.utils.mapper.ChatMapper.toRoomDTO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final MessageRepository messageRepository;
    private final ChatUserRepository chatUserRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final BrokerService brokerService;
    private static final String SORT_BY_DATE = "date";
    private static final String URL_REGEX = "(https?://[\\w\\-\\._~:/?#\\[\\]@!$&'()*+,;=%]+)";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

    @Transactional
    public ChatResponse.SendMessageDTO sendMessage(ChatRequest.SendMessageDTO requestDTO, Long senderId, String senderNickName) {
        validateChatAuthorization(senderId, requestDTO.chatRoomId());

        String messageId = UUID.randomUUID().toString();
        LinkMetadata metadata = extractMetadataIfApplicable(requestDTO);
        String senderProfileURL = getUserProfileURL(senderId);

        ChatRequest.MessageDTO messageDTO = toMessageDTO(requestDTO, senderNickName, messageId, metadata, senderProfileURL, senderId);
        sendMessageAsyncToBroker(requestDTO.chatRoomId(), messageDTO);

        return new ChatResponse.SendMessageDTO(messageId);
    }

    @Transactional(readOnly = true)
    public ChatResponse.FindChatRoomsDTO findChatRooms(Long userId) {
        // chatRoom이랑 Group도 같이 가져와야할 거 같음
        List<ChatUser> chatUsers = chatUserRepository.findByUserIdWithChatRoom(userId);

        List<ChatResponse.RoomDTO> roomDTOS = chatUsers.stream()
                .map(this::buildRoomDTO)
                .collect(Collectors.toList());

        return new ChatResponse.FindChatRoomsDTO(roomDTOS);
    }

    @Transactional
    public ChatResponse.FindMessagesInRoomDTO findMessageListInRoom(Long chatRoomId, Long userId, Pageable pageable) {
        // 권한 체크
        ChatUser chatUser = chatUserRepository.findByUserIdAndChatRoomIdWithChatRoom(userId, chatRoomId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_FORBIDDEN)
        );

        Page<Message> messages = messageRepository.findByChatRoomId(chatRoomId, pageable);
        List<ChatResponse.MessageDTO> messageDTOS = new ArrayList<>(messages.getContent().stream()
                .map(message -> {
                    List<ChatResponse.ChatObjectDTO> imageDTOS = message.getObjectURLs().stream()
                            .map(ChatResponse.ChatObjectDTO::new)
                            .toList();

                    // 링크 메타 데이터가 있으면 => 타입은 LINK
                    MessageType messageType = (message.getMetadata() != null) ? MessageType.LINK : message.getMessageType();

                    return new ChatResponse.MessageDTO(
                            message.getId(),
                            message.getNickName(),
                            message.getProfileURL(),
                            message.getContent(),
                            messageType,
                            imageDTOS,
                            message.getMetadata(),
                            message.getDate(),
                            message.getSenderId().equals(userId));
                })
                .toList());

        // messageDTOS 리스트의 순서를 역순으로 바꿈
        Collections.reverse(messageDTOS);

        // 마지막으로 읽은 메시지의 id와 index 업데이트
        if (!messageDTOS.isEmpty()) {
            ChatResponse.MessageDTO lastMessage = messageDTOS.get(messageDTOS.size() - 1);
            long chatNum = messageRepository.countByChatRoomId(chatRoomId);
            chatUser.updateLastMessage(lastMessage.messageId(), chatNum - 1);
        }

        // 채팅방에 들어가는 유저의 닉네임
        String nickName = userRepository.findNickname(userId);

        return new ChatResponse.FindMessagesInRoomDTO(chatUser.getChatRoom().getName(), chatUser.getLastMessageId(), nickName, messageDTOS);
    }

    @Transactional
    public ChatResponse.FindChatRoomDrawerDTO findChatRoomDrawer(Long chatRoomId, Long userId) {
        // 권한 체크
        validateChatAuthorization(userId, chatRoomId);

        // 채팅방에 참여한 유저
        List<ChatResponse.ChatUserDTO> chatUserDTOS = chatRoomRepository.findUsersByChatRoomIdExcludingRole(chatRoomId, GroupRole.TEMP).stream()
                .map(user -> new ChatResponse.ChatUserDTO(
                        user.getId(),
                        user.getNickname(),
                        user.getProfileURL()))
                .toList();

        // 채팅방의 S3 객체 => 처음 조회해오는 객체는 6개로 고정
        Pageable pageable = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, SORT_BY_DATE));
        List<ChatResponse.ImageObjectDTO> imageObjectDTOS = findImageObjects(chatRoomId, userId, pageable).images();

        return new ChatResponse.FindChatRoomDrawerDTO(imageObjectDTOS, chatUserDTOS);
    }

    @Transactional
    public ChatResponse.FindImageObjectsDTO findImageObjects(Long chatRoomId, Long userId, Pageable pageable) {
        // 권한 체크
        validateChatAuthorization(userId, chatRoomId);

        List<ChatResponse.ImageObjectDTO> imageObjectDTOS = new ArrayList<>();

        Page<Message> messages = messageRepository.findByChatRoomIdAndMessageType(chatRoomId, MessageType.IMAGE, pageable);
        messages.getContent().forEach(message -> {
            List<ChatResponse.ChatObjectDTO> chatObjectDTOS = message.getObjectURLs().stream()
                    .map(ChatResponse.ChatObjectDTO::new)
                    .toList();

            ChatResponse.ImageObjectDTO imageObjectDTO = new ChatResponse.ImageObjectDTO(
                    message.getId(),
                    message.getNickName(),
                    message.getProfileURL(),
                    chatObjectDTOS,
                    message.getDate());

            imageObjectDTOS.add(imageObjectDTO);
        });

        return new ChatResponse.FindImageObjectsDTO(imageObjectDTOS, messages.isLast());
    }

    @Transactional
    public ChatResponse.FindFileObjects findFileObjects(Long chatRoomId, Long userId, Pageable pageable) {
        // 권한 체크
        validateChatAuthorization(userId, chatRoomId);

        List<ChatResponse.FileObjectDTO> fileObjectDTOS = new ArrayList<>();

        Page<Message> messages = messageRepository.findByChatRoomIdAndMessageType(chatRoomId, MessageType.FILE, pageable);
        messages.getContent().forEach(message -> {
            List<ChatResponse.ChatObjectDTO> chatObjectDTOS = message.getObjectURLs().stream()
                    .map(ChatResponse.ChatObjectDTO::new)
                    .toList();

            ChatResponse.FileObjectDTO fileObjectDTO = new ChatResponse.FileObjectDTO(
                    message.getId(),
                    message.getContent(),
                    chatObjectDTOS,
                    message.getDate());

            fileObjectDTOS.add(fileObjectDTO);
        });

        return new ChatResponse.FindFileObjects(fileObjectDTOS, messages.isLast());
    }

    @Transactional
    public ChatResponse.FindLinkObjects findLinkObjects(Long chatRoomId, Long userId, Pageable pageable) {
        // 권한 체크
        validateChatAuthorization(userId, chatRoomId);

        List<ChatResponse.LinkObjectDTO> linkObjectDTOS = new ArrayList<>();

        Page<Message> messages = messageRepository.findByChatRoomIdAndMetadataOgUrlIsNotNull(chatRoomId, pageable);
        messages.getContent().forEach(message -> {
            LinkMetadata metadata = message.getMetadata();
            ChatResponse.LinkObjectDTO fileObjectDTO = new ChatResponse.LinkObjectDTO(
                    message.getId(),
                    Optional.ofNullable(metadata).map(LinkMetadata::getTitle).orElse(null),
                    Optional.ofNullable(metadata).map(LinkMetadata::getDescription).orElse(null),
                    Optional.ofNullable(metadata).map(LinkMetadata::getImage).orElse(null),
                    Optional.ofNullable(metadata).map(LinkMetadata::getOgUrl).orElse(null),
                    message.getDate());

            linkObjectDTOS.add(fileObjectDTO);
        });

        return new ChatResponse.FindLinkObjects(linkObjectDTOS, messages.isLast());
    }

    @Transactional
    public ChatResponse.ReadMessageDTO readMessage(String messageId) {
        // 권한 체크
        Message message = messageRepository.findById(messageId).orElseThrow(
                () -> new CustomException(ExceptionCode.MESSAGE_NOT_FOUND)
        );

        ChatUser chatUser = validateChatAuthorization(message.getSenderId(), message.getChatRoomId());
        chatUser.updateLastMessage(message.getId(), chatUser.getLastMessageIdx() + 1);

        return new ChatResponse.ReadMessageDTO(messageId);
    }

    // 채팅방에 들어와있는지 여부 체크
    private ChatUser validateChatAuthorization(Long senderId, Long chatRoomId) {
        return chatUserRepository.findByUserIdAndChatRoomId(senderId, chatRoomId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_FORBIDDEN));
    }

    private String getUserProfileURL(Long senderId) {
        return userRepository.findProfileURL(senderId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );
    }

    private LinkMetadata extractMetadataIfApplicable(ChatRequest.SendMessageDTO requestDTO) {
        if (requestDTO.messageType() == MessageType.TEXT) {
            String linkURL = extractFirstURL(requestDTO.content());
            return (linkURL != null) ? MetaDataUtils.fetchMetadata(linkURL) : null;
        }
        return null;
    }

    private void sendMessageAsyncToBroker(Long chatRoomId, ChatRequest.MessageDTO messageDTO) {
        CompletableFuture.runAsync(() -> brokerService.produceChatToRoom(chatRoomId, messageDTO));
    }

    private String extractFirstURL(String content) {
        Matcher matcher = URL_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    private ChatResponse.RoomDTO buildRoomDTO(ChatUser chatUser) {
        String lastMessageId = chatUser.getLastMessageId();
        MessageDetailDTO lastMessageDetails = fetchLastMessageDetails(lastMessageId);

        Long lastReadMessageIdx = chatUser.getLastMessageIdx();
        long lastReadMessageOffset = calculateMessageOffset(chatUser.getChatRoom().getId(), lastReadMessageIdx);

        return toRoomDTO(chatUser, lastMessageDetails.content(), lastMessageDetails.date(), lastReadMessageOffset);
    }

    private MessageDetailDTO fetchLastMessageDetails(String lastMessageId) {
        if (lastMessageId == null) {
            return new MessageDetailDTO(null, null);
        }

        return messageRepository.findById(lastMessageId)
                .map(message -> new MessageDetailDTO(message.getContent(), message.getDate()))
                .orElse(new MessageDetailDTO(null, null));
    }

    private long calculateMessageOffset(Long chatRoomId, Long lastReadMessageIdx) {
        long totalMessages = messageRepository.countByChatRoomId(chatRoomId);
        long totalPages = totalMessages != 0L ? (totalMessages / 50) : 0L;

        if (lastReadMessageIdx == null || lastReadMessageIdx == 0L) {
            return totalPages;
        }

        long lastReadPage = lastReadMessageIdx / 50;
        return totalPages - lastReadPage;
    }
}