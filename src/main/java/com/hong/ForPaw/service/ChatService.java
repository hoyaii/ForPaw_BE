package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.ChatRequest;
import com.hong.ForPaw.controller.DTO.ChatResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Chat.ChatUser;
import com.hong.ForPaw.domain.Chat.Message;
import com.hong.ForPaw.domain.Chat.MessageType;
import com.hong.ForPaw.repository.Chat.ChatRoomRepository;
import com.hong.ForPaw.repository.Chat.ChatUserRepository;
import com.hong.ForPaw.repository.Chat.MessageRepository;
import com.hong.ForPaw.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

    @Transactional
    public ChatResponse.SendMessageDTO sendMessage(ChatRequest.SendMessageDTO requestDTO, Long senderId, String senderNickName){
        // 권한 체크
        checkChatAuthority(senderId, requestDTO.chatRoomId());

        String profileURL = userRepository.findProfileURL(senderId).orElseThrow(
                () -> new CustomException(ExceptionCode.USER_NOT_FOUND)
        );

        // 전송을 위한 메시지 DTO
        String messageId = UUID.randomUUID().toString();
        LocalDateTime sendDate = LocalDateTime.now();

        ChatRequest.MessageDTO messageDTO = new ChatRequest.MessageDTO(
                messageId,
                senderNickName,
                profileURL,
                requestDTO.content(),
                requestDTO.messageType(),
                requestDTO.objects(),
                sendDate,
                requestDTO.chatRoomId(),
                senderId
        );

        // 메시지 브로커에 비동기로 전송 (알람과 이미지는 비동기로 처리)
        CompletableFuture.runAsync(() -> brokerService.produceChatToRoom(requestDTO.chatRoomId(), messageDTO));

        return new ChatResponse.SendMessageDTO(messageId);
    }

    @Transactional
    public ChatResponse.FindChatRoomListDTO findChatRoomList(Long userId){
        // chatRoom을 패치조인
        List<ChatUser> chatUsers = chatUserRepository.findByUserIdWithChatRoom(userId);

        List<ChatResponse.RoomDTO> roomDTOS = chatUsers.stream()
                .map(chatUser -> {
                    // 마지막으로 읽은 메시지
                    String lastMessageId = chatUser.getLastMessageId();
                    String lastMessageContent = null;
                    LocalDateTime lastMessageDate = null;

                    // 마지막으로 읽은 페이지 (인덱스/50)
                    Long lastReadMessageIdx = chatUser.getLastMessageIdx();
                    long offset = 0L;

                    // 채팅방에서 메시지를 읽은 적이 있다면
                    if (lastMessageId != null) {
                        Optional<Message> lastMessageOP = messageRepository.findById(lastMessageId);
                        lastMessageContent = lastMessageOP.map(Message::getContent).orElse(null);
                        lastMessageDate = lastMessageOP.map(Message::getDate).orElse(null);

                        long chatNum = messageRepository.countByChatRoomId(chatUser.getChatRoom().getId());
                        long entirePageNum = chatNum != 0L ? chatNum / 50 : 0L;
                        offset = lastReadMessageIdx != 0L ? entirePageNum - (lastReadMessageIdx / 50) : entirePageNum;
                    }

                    return new ChatResponse.RoomDTO(
                            chatUser.getChatRoom().getId(),
                            chatUser.getChatRoom().getName(),
                            lastMessageContent,
                            lastMessageDate,
                            offset);
                })
                .collect(Collectors.toList());

        return new ChatResponse.FindChatRoomListDTO(roomDTOS);
    }

    @Transactional
    public ChatResponse.FindMessageListInRoomDTO findMessageListInRoom(Long chatRoomId, Long userId, Pageable pageable){
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

                    return new ChatResponse.MessageDTO(
                            message.getId(),
                            message.getNickName(),
                            message.getProfileURL(),
                            message.getContent(),
                            message.getMessageType(),
                            imageDTOS,
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

        return new ChatResponse.FindMessageListInRoomDTO(chatUser.getChatRoom().getName(), chatUser.getLastMessageId(), nickName, messageDTOS);
    }

    @Transactional
    public ChatResponse.FindChatRoomDrawerDTO findChatRoomDrawer(Long chatRoomId, Long userId){
        // 권한 체크
        checkChatAuthority(userId, chatRoomId);

        // 채팅방에 참여한 유저
        List<ChatResponse.ChatUserDTO> chatUserDTOS = chatRoomRepository.findUsersByChatRoomId(chatRoomId).stream()
                .map(user -> new ChatResponse.ChatUserDTO(
                        user.getId(),
                        user.getNickName(),
                        user.getProfileURL()))
                .toList();

        // 채팅방의 S3 객체 => 처음 조회해오는 객체는 6개로 고정
        Pageable pageable = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, SORT_BY_DATE));
        List<ChatResponse.ImageObjectDTO> imageObjectDTOS = findImageObjectList(chatRoomId, userId, pageable);

        return new ChatResponse.FindChatRoomDrawerDTO(imageObjectDTOS, chatUserDTOS);
    }

    @Transactional
    public List<ChatResponse.ImageObjectDTO> findImageObjectList(Long chatRoomId, Long userId, Pageable pageable){
        // 권한 체크
        checkChatAuthority(userId, chatRoomId);

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

        // 역순으로 정렬
        Collections.reverse(imageObjectDTOS);

        return imageObjectDTOS;
    }

    @Transactional
    public List<ChatResponse.FileObjectDTO> findFileObjectList(Long chatRoomId, Long userId, Pageable pageable){
        // 권한 체크
        checkChatAuthority(userId, chatRoomId);

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

        // 역순으로 정렬
        Collections.reverse(fileObjectDTOS);

        return fileObjectDTOS;
    }

    @Transactional
    public ChatResponse.ReadMessageDTO readMessage(String messageId){
        // 권한 체크
        Message message = messageRepository.findById(messageId).orElseThrow(
                () -> new CustomException(ExceptionCode.MESSAGE_NOT_FOUND)
        );

        ChatUser chatUser = checkChatAuthority(message.getSenderId(), message.getChatRoomId());
        chatUser.updateLastMessage(message.getId(), chatUser.getLastMessageIdx() + 1);

        return new ChatResponse.ReadMessageDTO(messageId);
    }

    private ChatUser checkChatAuthority(Long userId, Long chatRoomId){
        // 채팅방에 들어와있는지 여부 체크
        Optional<ChatUser> chatUserOP = chatUserRepository.findByUserIdAndChatRoomId(userId, chatRoomId);
        if(chatUserOP.isEmpty()){
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }

        return chatUserOP.get();
    }
}