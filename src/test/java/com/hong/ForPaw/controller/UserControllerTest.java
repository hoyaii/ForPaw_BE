package com.hong.ForPaw.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.UserRequest;
import com.hong.ForPaw.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
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
    private UserService userService;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @Test
    public void 이메일_중복체크_성공() throws Exception {

        // given
        UserRequest.EmailDTO requestDTO = new UserRequest.EmailDTO("yg04077@naver.com");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/accounts/email/check")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : "+responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    public void 회원가입_이메일_코드전송_성공() throws Exception {

        // given
        UserRequest.EmailDTO requestDTO = new UserRequest.EmailDTO("yg04076@naver.com");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/auth/registration/code")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : "+responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    public void 비밀번호_재설정_이메일_코드전송_성공() throws Exception {

        // given
        UserRequest.EmailDTO requestDTO = new UserRequest.EmailDTO("yg04076@naver.com");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/auth/recovery/code")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : "+responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    public void 비밀번호_재설정_이메일_코드전송_실패() throws Exception {

        // given
        UserRequest.EmailDTO requestDTO = new UserRequest.EmailDTO("yg04077@naver.com");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/auth/recovery/code")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : "+responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    // 테스트 방법은 비밀번호_재설정_이메일_코드전송_성공() 테스트 실행 => 이메일에서 받은 코드 DTO에 입력 후 실행
    @Test
    public void 임시비밀번호_전송_성공() throws Exception {

        // given
        UserRequest.VerifyCodeDTO requestDTO = new UserRequest.VerifyCodeDTO("yg04076@naver.com", "iZ6Wk4Wu");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/auth/recovery/verify")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : "+responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    public void 임시비밀번호_전송_실패() throws Exception {

        // given
        // 잘못된 코드 입력
        UserRequest.VerifyCodeDTO requestDTO = new UserRequest.VerifyCodeDTO("yg04076@naver.com", "noAdv");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/auth/recovery/verify")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : "+responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 비밀번호_재설정_성공() throws Exception {

        // given
        UserRequest.ChangePasswordDTO requestDTO = new UserRequest.ChangePasswordDTO("pnu1234~", "pnu1234~", "446y*4MD");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                patch("/api/accounts/password")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : "+responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 비밀번호_재설정_실패_1() throws Exception {

        // given
        UserRequest.ChangePasswordDTO requestDTO = new UserRequest.ChangePasswordDTO("pnu1234~", "pnu1234~", "pnu123~");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                patch("/api/accounts/password")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : "+responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 비밀번호_재설정_실패_2() throws Exception {

        // given
        UserRequest.ChangePasswordDTO requestDTO = new UserRequest.ChangePasswordDTO("pnu1234~", "pnu124~", "pnu1234~");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                patch("/api/accounts/password")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : "+responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }
}