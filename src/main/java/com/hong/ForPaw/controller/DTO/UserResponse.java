package com.hong.ForPaw.controller.DTO;

public class UserResponse {
    public record JwtTokenDTO(String accessToken, String refreshToken) {}

    public record LoginDTO(String accessToken) {}

    public record EmailTokenDTO(String validationToken) {}

    public record AccessTokenDTO(String accessToken) {}
}
