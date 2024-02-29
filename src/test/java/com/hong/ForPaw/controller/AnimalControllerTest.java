package com.hong.ForPaw.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.UserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class AnimalControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 보호동물_목록_조회_성공() throws Exception {

        // given
        // when
        ResultActions result = mvc.perform(
                get("/api/animals")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "noticeSdt,desc")
        );

        // then
        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : "+responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 보호동물_조회_성공() throws Exception {

        // given
        Long id = 427342202400090L;

        // when
        ResultActions result = mvc.perform(
                get("/api/animals/" + id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
        );

        // then
        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : "+responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 보호동물_조회_실패() throws Exception {

        // given
        // 존재하지 않는 아이디
        Long id = 42734220L;

        // when
        ResultActions result = mvc.perform(
                get("/api/animals/" + id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
        );

        // then
        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : "+responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }
}