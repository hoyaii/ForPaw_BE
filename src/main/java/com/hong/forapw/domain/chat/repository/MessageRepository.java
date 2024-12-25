package com.hong.forapw.domain.chat.repository;

import com.hong.forapw.domain.chat.entity.Message;
import com.hong.forapw.domain.chat.constant.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    Page<Message> findByChatRoomId(Long chatRoomId, Pageable pageable);

    @Query("{ 'chatRoomId': ?0, 'objectURLs': { $exists: true, $ne: [], $not: { $size: 0 } } }")
    Page<Message> findByChatRoomIdWithObjects(Long chatRoomId, Pageable pageable);

    Page<Message> findByChatRoomIdAndMetadataOgUrlIsNotNull(Long chatRoomId, Pageable pageable);


    Page<Message> findByChatRoomIdAndMessageType(Long chatRoomId, MessageType messageType, Pageable pageable);


    Integer countByChatRoomId(Long chatRoomId);
}
