package com.hong.forapw.controller.dto.query;

import com.hong.forapw.domain.post.PostType;

public record PostTypeCountDTO(PostType postType, Long count) {
}
