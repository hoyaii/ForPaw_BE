package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.AlarmRequest;
import com.hong.ForPaw.controller.DTO.ChatRequest;
import com.hong.ForPaw.core.utils.MetaDataUtils;
import com.hong.ForPaw.domain.Alarm.Alarm;
import com.hong.ForPaw.domain.Alarm.AlarmType;
import com.hong.ForPaw.domain.Chat.LinkMetadata;
import com.hong.ForPaw.domain.Chat.Message;
import com.hong.ForPaw.domain.Chat.MessageType;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.repository.Alarm.AlarmRepository;
import com.hong.ForPaw.repository.Chat.ChatRoomRepository;
import com.hong.ForPaw.repository.Chat.MessageRepository;
import com.hong.ForPaw.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
public class BrokerService {

    private final AlarmRepository alarmRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    private final SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory;
    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final AlarmService alarmService;
    private final MessageConverter converter;
    private final EntityManager entityManager;

    private static final String CHAT_EXCHANGE = "chat.exchange";
    private static final String ALARM_EXCHANGE = "alarm.exchange";
    private static final String ROOM_QUEUE_PREFIX = "room.";
    private static final String USER_QUEUE_PREFIX = "user.";
    private static final String URL_REGEX = "(https?://[\\w\\-\\._~:/?#\\[\\]@!$&'()*+,;=%]+)";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

    @Transactional
    public void initChatListener(){
        chatRoomRepository.findAll()
                .forEach(chatRoom -> {
                    String queueName = ROOM_QUEUE_PREFIX + chatRoom.getId();
                    String listenerId = ROOM_QUEUE_PREFIX + chatRoom.getId();

                    registerDirectExQueue(CHAT_EXCHANGE, queueName);
                    registerChatListener(listenerId, queueName);
                });
    }

    @Transactional
    public void initAlarmListener(){
        userRepository.findAll()
                .forEach(user -> {
                    String queueName = USER_QUEUE_PREFIX + user.getId();
                    String listenerId = USER_QUEUE_PREFIX + user.getId();

                    registerDirectExQueue(ALARM_EXCHANGE, queueName);
                    registerAlarmListener(listenerId, queueName);
                });
    }

    public void registerDirectExchange(String exchangeName){
        DirectExchange fanoutExchange = new DirectExchange(exchangeName);
        amqpAdmin.declareExchange(fanoutExchange);
    }

    public void registerDirectExQueue(String exchangeName, String queueName){
        DirectExchange directExchange = new DirectExchange(exchangeName);

        Queue queue = new Queue(queueName, true);
        amqpAdmin.declareQueue(queue);

        // routingKey는 큐 이름과 동일
        Binding binding = BindingBuilder.bind(queue).to(directExchange).with(queueName);
        amqpAdmin.declareBinding(binding);
    }

    public void deleteQueue(String queueName){
        amqpAdmin.deleteQueue(queueName);
    }

    public void registerChatListener(String listenerId, String queueName) {
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(listenerId);
        endpoint.setQueueNames(queueName);
        endpoint.setMessageListener(m -> {
            ChatRequest.MessageDTO messageDTO = (ChatRequest.MessageDTO) converter.fromMessage(m);

            // 메시지 저장
            List<String> objectURLs = Optional.ofNullable(messageDTO.objects())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(ChatRequest.ChatObjectDTO::objectURL)
                    .toList();

            Message message = Message.builder()
                    .id(messageDTO.messageId())
                    .nickName(messageDTO.nickName())
                    .profileURL(messageDTO.profileURL())
                    .content(messageDTO.content())
                    .messageType(messageDTO.messageType())
                    .objectURLs(objectURLs)
                    .date(messageDTO.date())
                    .chatRoomId(messageDTO.chatRoomId())
                    .senderId(messageDTO.senderId())
                    .linkURL(messageDTO.linkURL())
                    .metadata(messageDTO.metadata())
                    .build();

            messageRepository.save(message);

            // 알람 전송
            chatRoomRepository.findUsersByChatRoomId(messageDTO.chatRoomId())
                    .forEach(user -> {
                        String content = "새로문 메시지: " + messageDTO.content();
                        String redirectURL = "chatRooms/" + messageDTO.chatRoomId();
                        LocalDateTime date = LocalDateTime.now();

                        AlarmRequest.AlarmDTO alarmDTO = new AlarmRequest.AlarmDTO(
                                user.getId(),
                                content,
                                redirectURL,
                                date,
                                AlarmType.CHATTING);

                        produceAlarmToUser(messageDTO.senderId(), alarmDTO);
                    });
        });

        rabbitListenerEndpointRegistry.registerListenerContainer(endpoint, rabbitListenerContainerFactory, true);
    }

    public void registerAlarmListener(String listenerId, String queueName){
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(listenerId);
        endpoint.setQueueNames(queueName);
        endpoint.setMessageListener(message -> {
            AlarmRequest.AlarmDTO alarmDTO = (AlarmRequest.AlarmDTO) converter.fromMessage(message);

            User receiver = entityManager.getReference(User.class, alarmDTO.receiverId());
            Alarm alarm = Alarm.builder()
                    .receiver(receiver)
                    .content(alarmDTO.content())
                    .redirectURL(alarmDTO.redirectURL())
                    .alarmType(alarmDTO.alarmType())
                    .build();

            // 알람 실시간 전송 후 저장
            alarmService.sendAlarmBySSE(alarm);
            alarmRepository.save(alarm);
        });

        rabbitListenerEndpointRegistry.registerListenerContainer(endpoint, rabbitListenerContainerFactory, true);
    }

    public void produceChatToRoom(Long chatRoomId, ChatRequest.MessageDTO message){
        String routingKey = ROOM_QUEUE_PREFIX + chatRoomId;
        rabbitTemplate.convertAndSend(CHAT_EXCHANGE, routingKey, message);
    }

    public void produceAlarmToUser(Long userId, AlarmRequest.AlarmDTO alarm) {
        String routingKey = USER_QUEUE_PREFIX + userId;
        rabbitTemplate.convertAndSend(ALARM_EXCHANGE, routingKey, alarm);
    }
}
