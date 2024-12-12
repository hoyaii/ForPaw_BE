package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.UserRequest;
import com.hong.forapw.controller.dto.UserResponse;
import com.hong.forapw.domain.inquiry.Inquiry;
import com.hong.forapw.domain.inquiry.InquiryStatus;
import com.hong.forapw.domain.user.AuthProvider;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.domain.user.UserRole;

public class UserMapper {

    private UserMapper() {
    }

    public static User buildUser(UserRequest.JoinDTO requestDTO, String password) {
        return User.builder()
                .name(requestDTO.name())
                .nickName(requestDTO.nickName())
                .email(requestDTO.email())
                .password(password)
                .role(requestDTO.isShelterOwns() ? UserRole.SHELTER : UserRole.USER)
                .profileURL(requestDTO.profileURL())
                .province(requestDTO.province())
                .district(requestDTO.district())
                .subDistrict(requestDTO.subDistrict())
                .authProvider(AuthProvider.LOCAL)
                .isMarketingAgreed(requestDTO.isMarketingAgreed())
                .build();
    }

    public static User buildUser(UserRequest.SocialJoinDTO requestDTO, String password) {
        return User.builder()
                .name(requestDTO.name())
                .nickName(requestDTO.nickName())
                .email(requestDTO.email())
                .password(password)
                .role(requestDTO.isShelterOwns() ? UserRole.SHELTER : UserRole.USER)
                .profileURL(requestDTO.profileURL())
                .province(requestDTO.province())
                .district(requestDTO.district())
                .subDistrict(requestDTO.subDistrict())
                .authProvider(requestDTO.authProvider())
                .isMarketingAgreed(requestDTO.isMarketingAgreed())
                .build();
    }

    public static UserResponse.ProfileDTO toProfileDTO(User user) {
        return new UserResponse.ProfileDTO(
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getProvince(),
                user.getDistrict(),
                user.getSubDistrict(),
                user.getProfileURL(),
                user.isSocialJoined(),
                user.isShelterOwns(),
                user.isMarketingAgreed());
    }
}
