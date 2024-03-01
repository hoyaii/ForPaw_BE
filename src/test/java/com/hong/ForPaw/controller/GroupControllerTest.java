package com.hong.ForPaw.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.AnimalRequest;
import com.hong.ForPaw.controller.DTO.GroupRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class GroupControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 그룹_생성_성공() throws Exception {

        // given
        GroupRequest.CreateGroupDTO requestDTO = new GroupRequest.CreateGroupDTO("동물 사랑 협회", "대구광역시", "수성구", "유기견들을 진료하는 모임입니다!", "봉사", "https://s3.xxxx.xx.com");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 그룹_정보_조회_성공() throws Exception {

        // given
        Long id = 1L;

        // when
        ResultActions result = mvc.perform(
                get("/api/groups/" + id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
        );

        // then
        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 그룹_정보_수정_성공() throws Exception {

        // given
        Long id = 1L;
        GroupRequest.UpdateGroupDTO requestDTO = new GroupRequest.UpdateGroupDTO("동물 사랑 협회2", "대구광역시", "수성구", "유기견들을 진료하는 모임입니다!", "봉사", "https://s3.xxxx.xx.com");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                patch("/api/groups/" + id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }
}