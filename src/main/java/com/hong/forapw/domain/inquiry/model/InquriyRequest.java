package com.hong.forapw.domain.inquiry.model;

import com.hong.forapw.domain.inquiry.constant.InquiryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class InquriyRequest {

    public record SubmitInquiry(
            @NotBlank(message = "제목을 입력해주세요.")
            String title,
            @NotBlank(message = "문의 내용을 입력해주세요.")
            String description,
            @NotBlank(message = "답변을 받을 이메일 입력해주세요.")
            String contactMail,
            String imageURL,
            InquiryType inquiryType) {
    }

    public record UpdateInquiry(
            @NotBlank(message = "제목을 입력해주세요.")
            String title,
            @NotBlank(message = "문의 내용을 입력해주세요.")
            String description,
            @NotBlank(message = "이메일을 입력해주세요.")
            @Pattern(regexp = "^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "올바른 이메일 형식을 입력해주세요.")
            String contactMail) {
    }
}
