package com.hong.ForPaw.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.UserRequest;
import com.hong.ForPaw.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    UserService userService;

    @Autowired
    private ObjectMapper om;

    // 테스트 시 ddl=create로 하고 써야한다.
    @Test
    public void 로그인_성공() throws Exception {
        // given
        UserRequest.LoginDTO requestDTO = new UserRequest.LoginDTO("yg04076@naver.com", "hong1234");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        // then
        result.andExpect(status().isOk()) // 상태 코드가 200인지 검증
                .andExpect(jsonPath("$.accessToken").exists()) // accessToken 필드가 응답 JSON에 존재하는지 검증
                .andExpect(cookie().exists("refreshToken")) // refreshToken 쿠키가 존재하는지 검증
                .andExpect(cookie().httpOnly("refreshToken", true)) // refreshToken 쿠키가 HttpOnly인지 검증
                .andExpect(header().exists("Set-Cookie")); // Set-Cookie 헤더가 존재하는지 검증
    }

    @DisplayName("사용자_회원가입_성공_test")
    @Test
    public void 회원가입_성공() throws Exception {

        // given
        UserRequest.JoinDTO requestDTO = new UserRequest.JoinDTO("hoyai@naver.com", "한홍", "호얘이", "대구", "수성구", "pnu1234!", "pnu1234!", "www.s3.1234.com");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        // then
        result.andExpect(jsonPath("$.success").value("true"));
        result.andExpect(jsonPath("$.message").value("Created"));
    }
}