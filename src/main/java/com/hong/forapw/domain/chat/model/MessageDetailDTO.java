package com.hong.forapw.domain.chat.model;

import java.time.LocalDateTime;

public record MessageDetailDTO(String content, LocalDateTime date) {
}
