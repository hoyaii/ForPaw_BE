package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.UserRequest;
import com.hong.forapw.controller.dto.UserResponse;
import com.hong.forapw.domain.inquiry.Inquiry;
import com.hong.forapw.domain.inquiry.InquiryStatus;
import com.hong.forapw.domain.user.User;

import java.util.Optional;

public class InquiryMapper {

    private InquiryMapper() {
    }

    public static Inquiry buildInquiry(UserRequest.SubmitInquiry requestDTO, InquiryStatus status, User user) {
        return Inquiry.builder()
                .questioner(user)
                .title(requestDTO.title())
                .description(requestDTO.description())
                .contactMail(requestDTO.contactMail())
                .status(status)
                .type(requestDTO.inquiryType())
                .imageURL(requestDTO.imageURL())
                .build();
    }

    public static UserResponse.InquiryDTO toInquiryDTO(Inquiry inquiry) {
        UserResponse.AnswerDTO answerDTO = Optional.ofNullable(inquiry.getAnswer())
                .map(answer -> toAnswerDTO(inquiry))
                .orElse(null);

        return new UserResponse.InquiryDTO(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getDescription(),
                inquiry.getStatus(),
                inquiry.getImageURL(),
                inquiry.getType(),
                inquiry.getCreatedDate(),
                answerDTO);
    }

    private static UserResponse.AnswerDTO toAnswerDTO(Inquiry inquiry) {
        return new UserResponse.AnswerDTO(
                inquiry.getAnswer(),
                inquiry.getAnswerer().getName());
    }
}
