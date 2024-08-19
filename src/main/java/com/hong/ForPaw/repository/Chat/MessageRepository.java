package com.hong.ForPaw.repository.Chat;

import com.hong.ForPaw.domain.Chat.Message;
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

    Integer countByChatRoomId(Long chatRoomId);
}
