package com.hong.ForPaw.controller.DTO;

public class UserResponse {
    public record JwtTokenDTO(String accessToken, String refreshToken) {}

    public record EmailTokenDTO(String validationToken) {}

    public record AccessTokenDTO(String accessToken) {}

    public record ProfileDTO(String name, String nickName, String region, String subRegion, String profileURL) {}
}
