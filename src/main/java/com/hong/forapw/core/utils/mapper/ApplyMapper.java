package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.ApplyRequest;
import com.hong.forapw.controller.dto.ApplyResponse;
import com.hong.forapw.domain.animal.Animal;
import com.hong.forapw.domain.apply.Apply;
import com.hong.forapw.domain.apply.ApplyStatus;
import com.hong.forapw.domain.user.User;

public class ApplyMapper {

    private ApplyMapper() {
    }

    public static Apply buildApply(ApplyRequest.ApplyAdoptionDTO requestDTO, User user, Animal animal) {
        return Apply.builder()
                .user(user)
                .animal(animal)
                .status(ApplyStatus.PROCESSING)
                .name(requestDTO.name())
                .tel(requestDTO.tel())
                .roadNameAddress(requestDTO.roadNameAddress())
                .addressDetail(requestDTO.addressDetail())
                .zipCode(requestDTO.zipCode())
                .build();
    }

    public static ApplyResponse.ApplyDTO toApplyDTO(Apply apply) {
        return new ApplyResponse.ApplyDTO(
                apply.getId(),
                apply.getAnimal().getId(),
                apply.getAnimal().getName(),
                apply.getAnimal().getKind(),
                apply.getAnimal().getGender(),
                apply.getAnimal().getAge(),
                apply.getName(),
                apply.getTel(),
                apply.getRoadNameAddress(),
                apply.getAddressDetail(),
                apply.getZipCode(),
                apply.getStatus());
    }
}
