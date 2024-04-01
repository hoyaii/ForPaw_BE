package com.hong.ForPaw.repository.Chat;

import com.hong.ForPaw.domain.Chat.ChatImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatImageRepository extends JpaRepository<ChatImage, Long> {

    Page<ChatImage> findByChatRoomId(Long chatRoomId, Pageable pageable);
}