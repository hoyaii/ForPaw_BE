package com.hong.forapw.domain.apply;

import com.hong.forapw.domain.apply.model.ApplyRequest;
import com.hong.forapw.domain.apply.model.ApplyResponse;
import com.hong.forapw.domain.animal.entity.Animal;
import com.hong.forapw.domain.apply.entity.Apply;
import com.hong.forapw.domain.apply.constant.ApplyStatus;
import com.hong.forapw.domain.user.entity.User;

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
