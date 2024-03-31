package com.hong.ForPaw.controller.DTO;

public class GoogleOauthDTO {

    public record TokenDTO(String access_token, Long expires_in, String token_type, String scope, String refresh_token) {}

    public record UserInfoDTO(String id,
                              String email,
                              String nickname) {}
}
