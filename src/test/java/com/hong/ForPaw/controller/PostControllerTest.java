package com.hong.ForPaw.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.GroupRequest;
import com.hong.ForPaw.controller.DTO.PostRequest;
import com.hong.ForPaw.domain.Post.Type;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class PostControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Test
    @WithUserDetails(value = "yg04076@naver.com")
    public void 게시글_작성_성공() throws Exception {

        // given
        List<PostRequest.PostImageDTO> imageDTOS = new ArrayList<>();
        imageDTOS.add(new PostRequest.PostImageDTO("https://example.com/image1.jpg"));
        imageDTOS.add(new PostRequest.PostImageDTO("https://example.com/image2.jpg"));

        PostRequest.CreatePostDTO requestDTO = new PostRequest.CreatePostDTO("불독 입양 후기 올립니다!", Type.adoption, "입양 2일차 입니다!", imageDTOS);
        String requestBody = om.writeValueAsString(requestDTO);

        // when
        ResultActions result = mvc.perform(
                post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestBody)
        );

        String responseBody = result.andReturn().getResponse().getContentAsString();
        System.out.println("테스트 : " + responseBody);

        result.andExpect(jsonPath("$.success").value("true"));
    }
}