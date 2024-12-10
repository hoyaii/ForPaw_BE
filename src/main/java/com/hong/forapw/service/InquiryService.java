package com.hong.forapw.service;

import com.hong.forapw.controller.dto.UserRequest;
import com.hong.forapw.controller.dto.UserResponse;
import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.domain.inquiry.Inquiry;
import com.hong.forapw.domain.inquiry.InquiryStatus;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.repository.inquiry.InquiryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.hong.forapw.core.utils.mapper.UserMapper.buildInquiry;
import static com.hong.forapw.core.utils.mapper.UserMapper.toInquiryDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final EntityManager entityManager;

    @Transactional
    public UserResponse.SubmitInquiryDTO submitInquiry(UserRequest.SubmitInquiry requestDTO, Long userId) {
        User user = entityManager.getReference(User.class, userId);
        Inquiry inquiry = buildInquiry(requestDTO, InquiryStatus.PROCESSING, user);

        inquiryRepository.save(inquiry);

        return new UserResponse.SubmitInquiryDTO(inquiry.getId());
    }

    @Transactional
    public void updateInquiry(UserRequest.UpdateInquiry requestDTO, Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        checkAuthority(userId, inquiry.getQuestioner());
        inquiry.updateInquiry(requestDTO.title(), requestDTO.description(), requestDTO.contactMail());
    }

    @Transactional(readOnly = true)
    public UserResponse.FindInquiryListDTO findInquiryList(Long userId) {
        List<Inquiry> customerInquiries = inquiryRepository.findAllByQuestionerId(userId);

        List<UserResponse.InquiryDTO> inquiryDTOS = customerInquiries.stream()
                .map(inquiry -> {
                    UserResponse.AnswerDTO answerDTO = null;
                    if (inquiry.getAnswer() != null) {
                        answerDTO = new UserResponse.AnswerDTO(
                                inquiry.getAnswer(),
                                inquiry.getAnswerer().getName()
                        );
                    }
                    return toInquiryDTO(inquiry, answerDTO);
                })
                .toList();

        return new UserResponse.FindInquiryListDTO(inquiryDTOS);
    }

    private void checkAuthority(Long userId, User writer) {
        if (writer.isNotSameUser(userId)) {
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }
}
