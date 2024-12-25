package com.hong.forapw.domain.inquiry;

import com.hong.forapw.domain.inquiry.model.InquiryResponse;
import com.hong.forapw.domain.inquiry.model.InquriyRequest;
import com.hong.forapw.domain.user.model.UserResponse;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.inquiry.entity.Inquiry;
import com.hong.forapw.domain.inquiry.constant.InquiryStatus;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.hong.forapw.domain.inquiry.InquiryMapper.buildInquiry;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    @Transactional
    public InquiryResponse.SubmitInquiryDTO submitInquiry(InquriyRequest.SubmitInquiry requestDTO, Long userId) {
        User submitter = userRepository.getReferenceById(userId);
        Inquiry inquiry = buildInquiry(requestDTO, InquiryStatus.PROCESSING, submitter);

        inquiryRepository.save(inquiry);

        return new InquiryResponse.SubmitInquiryDTO(inquiry.getId());
    }

    @Transactional
    public void updateInquiry(InquriyRequest.UpdateInquiry requestDTO, Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(ExceptionCode.INQUIRY_NOT_FOUND)
        );

        checkAuthority(userId, inquiry.getQuestioner());
        inquiry.updateInquiry(requestDTO.title(), requestDTO.description(), requestDTO.contactMail());
    }

    public InquiryResponse.FindInquiryListDTO findInquiries(Long userId) {
        List<Inquiry> inquiries = inquiryRepository.findAllByQuestionerId(userId);
        List<UserResponse.InquiryDTO> inquiryDTOS = inquiries.stream()
                .map(InquiryMapper::toInquiryDTO)
                .toList();

        return new InquiryResponse.FindInquiryListDTO(inquiryDTOS);
    }

    private void checkAuthority(Long userId, User writer) {
        if (writer.isNotSameUser(userId)) {
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }
}
