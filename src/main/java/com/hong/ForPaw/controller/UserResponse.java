package com.hong.ForPaw.controller;

public class UserResponse {
    public record TokenDTO(String accessToken, String refreshToken) {}

    public record LoginDTO(String accessToken) {}
}
