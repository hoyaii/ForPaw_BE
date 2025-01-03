package com.hong.forapw.domain.apply.model;

import com.hong.forapw.domain.apply.constant.ApplyStatus;

import java.util.List;

public class ApplyResponse {

    public record FindApplyListDTO(List<ApplyDTO> applies) {
    }

    public record CreateApplyDTO(Long id) {
    }

    public record ApplyDTO(Long applyId,
                           Long animalId,
                           String animalName,
                           String kind,
                           String gender,
                           String age,
                           String userName,
                           String tel,
                           String roadNameAddress,
                           String addressDetail,
                           String zipCode,
                           ApplyStatus status) {
    }
}
