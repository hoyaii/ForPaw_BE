package com.hong.ForPaw.controller.DTO;

public class UserResponse {
    public record TokenDTO(String accessToken, String refreshToken) {}

    public record LoginDTO(String accessToken) {}
}
