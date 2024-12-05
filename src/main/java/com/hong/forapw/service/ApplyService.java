package com.hong.forapw.service;

import com.hong.forapw.controller.dto.AnimalResponse;
import com.hong.forapw.controller.dto.ApplyRequest;
import com.hong.forapw.controller.dto.ApplyResponse;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.domain.animal.Animal;
import com.hong.forapw.domain.apply.Apply;
import com.hong.forapw.domain.apply.ApplyStatus;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.repository.ApplyRepository;
import com.hong.forapw.repository.animal.AnimalRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ApplyService {

    private final ApplyRepository applyRepository;
    private final AnimalRepository animalRepository;
    private final EntityManager entityManager;

    @Transactional
    public ApplyResponse.CreateApplyDTO applyAdoption(ApplyRequest.ApplyAdoptionDTO requestDTO, Long userId, Long animalId) {
        // 동물이 존재하지 않으면 에러
        Animal animal = animalRepository.findById(animalId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );

        // 이미 입양된 동물이면 에러
        if (animal.isAdopted()) {
            throw new CustomException(ExceptionCode.ANIMAL_NOT_FOUND);
        }

        // 이미 지원하였으면 에러
        if (applyRepository.existsByUserIdAndAnimalId(userId, animalId)) {
            throw new CustomException(ExceptionCode.ANIMAL_ALREADY_APPLY);
        }

        User userRef = entityManager.getReference(User.class, userId);
        Apply apply = Apply.builder()
                .user(userRef)
                .animal(animal)
                .status(ApplyStatus.PROCESSING)
                .name(requestDTO.name())
                .tel(requestDTO.tel())
                .roadNameAddress(requestDTO.roadNameAddress())
                .addressDetail(requestDTO.addressDetail())
                .zipCode(requestDTO.zipCode())
                .build();

        applyRepository.save(apply);

        // 동물의 문의 횟수 증가
        animalRepository.incrementInquiryNumById(animalId);

        return new ApplyResponse.CreateApplyDTO(apply.getId());
    }

    @Transactional(readOnly = true)
    public ApplyResponse.FindApplyListDTO findApplyList(Long userId) {
        List<Apply> applies = applyRepository.findAllByUserIdWithAnimal(userId);

        List<ApplyResponse.ApplyDTO> applyDTOS = applies.stream()
                .map(apply -> new ApplyResponse.ApplyDTO(
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
                        apply.getStatus()))
                .collect(Collectors.toList());

        return new ApplyResponse.FindApplyListDTO(applyDTOS);
    }

    @Transactional
    public void updateApply(ApplyRequest.UpdateApplyDTO requestDTO, Long applyId, Long userId) {
        // 지원하지 않았거나, 권한이 없으면 에러
        if (!applyRepository.existsByApplyIdAndUserId(applyId, userId)) {
            throw new CustomException(ExceptionCode.APPLY_NOT_FOUND);
        }

        Apply apply = applyRepository.findById(applyId).orElseThrow(
                () -> new CustomException(ExceptionCode.APPLY_NOT_FOUND)
        );

        apply.updateApply(requestDTO.name(),
                requestDTO.tel(),
                requestDTO.roadNameAddress(),
                requestDTO.addressDetail(),
                requestDTO.zipCode());
    }

    @Transactional
    public void deleteApply(Long applyId, Long userId) {
        // 지원하지 않았거나, 권한이 없으면 에러
        if (!applyRepository.existsByApplyIdAndUserId(applyId, userId)) {
            throw new CustomException(ExceptionCode.APPLY_NOT_FOUND);
        }

        // 동물의 문의 횟수 감소
        Animal animal = applyRepository.findAnimalIdById(applyId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );
        animal.decrementInquiryNum();

        applyRepository.deleteById(applyId);
    }
}
