package com.hong.forapw.controller.dto;


import com.hong.forapw.domain.inquiry.InquiryStatus;
import com.hong.forapw.domain.District;
import com.hong.forapw.domain.inquiry.InquiryType;
import com.hong.forapw.domain.Province;

import java.time.LocalDateTime;
import java.util.List;

public class UserResponse {
    public record LoginDTO(String accessToken) {
    }

    public record CheckAccountExistDTO(boolean isValid) {
    }

    public record CheckLocalAccountExistDTO(boolean isValid, boolean isLocal) {
    }

    public record CheckNickNameDTO(boolean isDuplicate) {
    }

    public record AccessTokenDTO(String accessToken) {
    }

    public record ProfileDTO(String email,
                             String name,
                             String nickName,
                             Province province,
                             District district,
                             String subDistrict,
                             String profileURL,
                             boolean isSocialJoined,
                             boolean isShelterOwns,
                             boolean isMarketingAgreed) {
    }

    public record VerifyEmailCodeDTO(boolean isMatching) {
    }

    public record SubmitInquiryDTO(Long id) {
    }

    public record FindInquiryListDTO(List<InquiryDTO> inquiries) {
    }

    public record InquiryDTO(Long id,
                             String title,
                             String description,
                             InquiryStatus status,
                             String imageURL,
                             InquiryType inquiryType,
                             LocalDateTime createdDate,
                             AnswerDTO answer) {
    }

    public record AnswerDTO(String content,
                            String answeredBy) {
    }

    public record VerifyPasswordDTO(boolean isMatching) {
    }

    public record ValidateAccessTokenDTO(String profile) {
    }

    public record FindCommunityRecord(String nickName,
                                      String email,
                                      Long postNum,
                                      Long commentNum,
                                      Long questionNum,
                                      Long answerNum) {
    }
}
