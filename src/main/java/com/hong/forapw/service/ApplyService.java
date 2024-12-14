package com.hong.forapw.service;

import com.hong.forapw.controller.dto.ApplyRequest;
import com.hong.forapw.controller.dto.ApplyResponse;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.core.utils.mapper.ApplyMapper;
import com.hong.forapw.domain.animal.Animal;
import com.hong.forapw.domain.apply.Apply;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.repository.ApplyRepository;
import com.hong.forapw.repository.UserRepository;
import com.hong.forapw.repository.animal.AnimalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.hong.forapw.core.utils.mapper.ApplyMapper.buildApply;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ApplyService {

    private final ApplyRepository applyRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApplyResponse.CreateApplyDTO applyAdoption(ApplyRequest.ApplyAdoptionDTO requestDTO, Long userId, Long animalId) {
        Animal animal = animalRepository.findById(animalId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );

        validateAnimalNotAdopted(animal);
        validateNoPreviousApplication(userId, animalId);

        User user = userRepository.getReferenceById(userId);
        Apply apply = buildApply(requestDTO, user, animal);
        applyRepository.save(apply);

        animal.incrementInquiryNum();

        return new ApplyResponse.CreateApplyDTO(apply.getId());
    }

    public ApplyResponse.FindApplyListDTO findApplyList(Long userId) {
        List<Apply> applies = applyRepository.findAllByUserIdWithAnimal(userId);

        List<ApplyResponse.ApplyDTO> applyDTOS = applies.stream()
                .map(ApplyMapper::toApplyDTO)
                .toList();

        return new ApplyResponse.FindApplyListDTO(applyDTOS);
    }

    @Transactional
    public void updateApply(ApplyRequest.UpdateApplyDTO requestDTO, Long applyId, Long userId) {
        validateUserIsApplicant(applyId, userId);

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
        validateUserIsApplicant(applyId, userId);

        Animal animal = applyRepository.findAnimalIdById(applyId).orElseThrow(
                () -> new CustomException(ExceptionCode.ANIMAL_NOT_FOUND)
        );
        animal.decrementInquiryNum();

        applyRepository.deleteById(applyId);
    }

    private void validateAnimalNotAdopted(Animal animal) {
        if (animal.isAdopted()) {
            throw new CustomException(ExceptionCode.ANIMAL_ALREADY_ADOPTED);
        }
    }

    private void validateNoPreviousApplication(Long userId, Long animalId) {
        if (applyRepository.existsByUserIdAndAnimalId(userId, animalId)) {
            throw new CustomException(ExceptionCode.ANIMAL_ALREADY_APPLY);
        }
    }

    private void validateUserIsApplicant(Long applyId, Long userId) {
        if (!applyRepository.existsByApplyIdAndUserId(applyId, userId)) {
            throw new CustomException(ExceptionCode.APPLY_NOT_FOUND);
        }
    }
}
