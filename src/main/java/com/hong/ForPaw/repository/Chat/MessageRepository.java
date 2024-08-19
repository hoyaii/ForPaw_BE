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

    @Query("{ '$or': [ { 'objectURLs': { $exists: true, $ne: [], $not: { $size: 0 } } }, { 'linkURL': { $ne: null } } ], 'chatRoomId': ?0 }")
    Page<Message> findByChatRoomIdWithObjectsOrLink(Long chatRoomId, Pageable pageable);

    Integer countByChatRoomId(Long chatRoomId);
}
