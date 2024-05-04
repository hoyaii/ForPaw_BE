package com.hong.ForPaw.controller.DTO;


import com.hong.ForPaw.domain.Inquiry.InquiryStatus;

import java.time.LocalDateTime;
import java.util.List;

public class UserResponse {
    public record LoginDTO(String accessToken) {}

    public record KakaoLoginDTO(String accessToken, String email) {}

    public record GoogleLoginDTO(String accessToken, String email) {}

    public record AccessTokenDTO(String accessToken) {}

    public record ProfileDTO(String name,
                             String nickName,
                             String province,
                             String district,
                             String subDistrict,
                             String profileURL) {}

    public record SubmitInquiryDTO(Long id) {}

    public record FindInquiryListDTO(List<InquiryDTO> inquiries){}

    public record InquiryDTO(Long id,
                             String title,
                             InquiryStatus inquiryStatus,
                             LocalDateTime createdDate) {}

    public record FindInquiryByIdDTO(){}
}
