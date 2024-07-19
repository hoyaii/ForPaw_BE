package com.hong.ForPaw.controller.DTO;


import com.hong.ForPaw.domain.Inquiry.InquiryStatus;
import com.hong.ForPaw.domain.District;
import com.hong.ForPaw.domain.Province;
import com.hong.ForPaw.domain.User.UserRole;

import java.time.LocalDateTime;
import java.util.List;

public class UserResponse {
    public record LoginDTO(String accessToken) {}

    public record KakaoLoginDTO(String accessToken, String email) {}

    public record GoogleLoginDTO(String accessToken, String email) {}

    public record AccessTokenDTO(String accessToken) {}

    public record ProfileDTO(String email,
                             String name,
                             String nickName,
                             Province province,
                             District district,
                             String subDistrict,
                             String profileURL) {}

    public record SubmitInquiryDTO(Long id) {}

    public record FindInquiryListDTO(List<InquiryDTO> inquiries){}

    public record InquiryDTO(Long id,
                             String title,
                             InquiryStatus status,
                             LocalDateTime createdDate) {}

    public record FindInquiryByIdDTO(String title,
                                     String description,
                                     InquiryStatus status,
                                     LocalDateTime createdDate,
                                     List<AnswerDTO> answers){}

    public record AnswerDTO(Long id,
                            String content,
                            LocalDateTime answeredDate,
                            String answeredBy){}

    public record VerifyPasswordDTO(boolean isMatching){}
}
