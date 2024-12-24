package com.hong.forapw.domain.user;

import com.hong.forapw.domain.user.model.UserRequest;
import com.hong.forapw.domain.user.model.UserResponse;
import com.hong.forapw.domain.user.constant.AuthProvider;
import com.hong.forapw.domain.user.entity.User;
import com.hong.forapw.domain.user.constant.UserRole;

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
