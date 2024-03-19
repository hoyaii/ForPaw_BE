package com.hong.ForPaw.repository.Chat;

import com.hong.ForPaw.domain.Chat.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends MongoRepository<Message, Long> {

}
