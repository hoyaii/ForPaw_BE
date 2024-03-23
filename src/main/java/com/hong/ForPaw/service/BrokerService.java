package com.hong.ForPaw.service;

import com.hong.ForPaw.domain.Alarm.Alarm;
import com.hong.ForPaw.domain.Alarm.AlarmType;
import com.hong.ForPaw.domain.User.User;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrokerService {

    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    private final RabbitListenerContainerFactory<?> rabbitListenerContainerFactory;
    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final AlarmService alarmService;

    public void registerExchange(String exchangeName){
        FanoutExchange fanoutExchange = new FanoutExchange(exchangeName);
        amqpAdmin.declareExchange(fanoutExchange);
    }

    public void registerQueue(String exchangeName, String queueName){
        FanoutExchange fanoutExchange = new FanoutExchange(exchangeName);

        Queue userQueue = new Queue(queueName, true);
        amqpAdmin.declareQueue(userQueue);

        // 해당 그룹 채팅방의 Fanout Exchange와 큐를 바인딩
        Binding binding = BindingBuilder.bind(userQueue).to(fanoutExchange);
        amqpAdmin.declareBinding(binding);
    }

    public void registerListener(String listenerId, String queueName) {
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(listenerId);
        endpoint.setQueueNames(queueName);
        endpoint.setMessageListener(message -> {

        });

        rabbitListenerEndpointRegistry.registerListenerContainer(endpoint, rabbitListenerContainerFactory, true);
    }

    public void produceAlarm(String routingKey, Alarm alarm) {
        rabbitTemplate.convertAndSend("alarm.exchange", routingKey, alarm);
    }

    @RabbitListener(queues = "alarm.queue")
    public void consumeAlarm(Alarm alarm) {
        alarmService.send(alarm);
    }
}
