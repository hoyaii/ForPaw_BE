package com.hong.forapw.domain.inquiry.model;

import com.hong.forapw.domain.user.model.UserResponse;

import java.util.List;

public class InquiryResponse {

    public record SubmitInquiryDTO(Long id) {
    }

    public record FindInquiryListDTO(List<UserResponse.InquiryDTO> inquiries) {
    }
}
