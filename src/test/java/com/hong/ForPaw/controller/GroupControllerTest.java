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

    // 테스트 시나리오를 짜고 DB 세팅 후테스트 해야함! yg04076@naver.com은 그룹 생성자, yg040762@naver.com은 가입 신청자
    @Test
    @WithUserDetails(value = "yg040762@naver.com")
    public void 그룹_가입_신청하기_성공() throws Exception {

        // given
        Long groupId = 12L;
        GroupRequest.JoinGroupDTO requestDTO = new GroupRequest.JoinGroupDTO("안녕하세요! 가입 인사 드립니다 ^^");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/join")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    @WithUserDetails(value = "yg040762@naver.com")
    public void 그룹_가입_신청하기_실패_1() throws Exception {

        // given
        // 이미 신청한 그룹
        Long groupId = 12L;
        GroupRequest.JoinGroupDTO requestDTO = new GroupRequest.JoinGroupDTO("안녕하세요! 가입 인사 드립니다 ^^");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/join")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    @Test
    @WithUserDetails(value = "yg040762@naver.com")
    public void 그룹_가입_신청하기_실패_2() throws Exception {

        // given
        // 존재하지 않는 그룹
        Long groupId = 100L;
        GroupRequest.JoinGroupDTO requestDTO = new GroupRequest.JoinGroupDTO("안녕하세요! 가입 인사 드립니다 ^^");
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/join")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 그룹_가입_승인하기_성공() throws Exception {

        // given
        Long groupId = 12L;
        Long applicantId = 2L;

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/join/approve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("id", applicantId.toString())
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 그룹_가입_승인하기_실패_1() throws Exception {

        // given
        // 이미 승인한 신청
        Long groupId = 12L;
        Long applicantId = 2L;

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/join/approve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("id", applicantId.toString())
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 그룹_가입_승인하기_실패_2() throws Exception {

        // given
        // 존재하지 않는 그룹
        Long groupId = 100L;
        Long applicantId = 2L;

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/join/approve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("id", applicantId.toString())
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    @Test
    @WithUserDetails(value = "yg040763@naver.com") // 권한 없음
    public void 그룹_가입_승인하기_실패_3() throws Exception {

        // given
        Long groupId = 12L;
        Long applicantId = 2L;

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/join/approve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("id", applicantId.toString())
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 그룹_가입_승인하기_실패_4() throws Exception {

        // given
        // 신청한 적이 없는 경우
        Long groupId = 12L;
        Long applicantId = 3L;

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/join/approve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("id", applicantId.toString())
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 그룹_가입_거절하기_성공() throws Exception {

        // given
        Long groupId = 12L;
        Long applicantId = 3L;

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/join/reject")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("id", applicantId.toString())
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 그룹_가입_거절하기_실패_1() throws Exception {

        // given
        // 이미 거절한 신청
        Long groupId = 12L;
        Long applicantId = 3L;

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/join/reject")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("id", applicantId.toString())
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    @Test
    @WithUserDetails(value = "yg040763@naver.com") // 권한 없음
    public void 그룹_가입_거절하기_실패_2() throws Exception {

        // given
        Long groupId = 12L;
        Long applicantId = 3L;

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/join/reject")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("id", applicantId.toString())
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 그룹_가입_거절하기_실패_3() throws Exception {

        // given
        // 신청한 적이 없는 경우
        Long groupId = 12L;
        Long applicantId = 13L;

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/join/reject")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("id", applicantId.toString())
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 관심_그룹으로_등록_성공() throws Exception {

        // given
        Long groupId = 12L;

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/like")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 관심_그룹으로_등록_실패() throws Exception {

        // given
        // 존재하지 않는 그룹
        Long groupId = 100L;

        // when
        ResultActions result = mvc.perform(
                post("/api/groups/"+groupId+"/like")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("false"));
    }

    @Test
    @WithUserDetails(value = "yg040762@naver.com")
    public void 그룹_목록_조회_성공() throws Exception {

        // given
        String region = "대구광역시";

        // when
        ResultActions result = mvc.perform(
                get("/api/groups/")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .param("region", region)
        );

        // then
        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }
}