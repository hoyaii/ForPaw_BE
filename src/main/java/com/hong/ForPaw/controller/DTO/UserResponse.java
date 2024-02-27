package com.hong.ForPaw.controller.DTO;

public class UserResponse {
    public record LoginTokenDTO(String accessToken, String refreshToken) {}

    public record LoginDTO(String accessToken) {}

    public record EmailTokenDTO(String validationToken) {}
}
