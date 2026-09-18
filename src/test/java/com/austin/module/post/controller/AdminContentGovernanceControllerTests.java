package com.austin.module.post.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminContentGovernanceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ordinaryUserCannotListCommentsOrMeetupReviews() throws Exception {
        mockMvc.perform(get("/api/v1/admin/post-comments").with(user("1")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/meetup-reviews").with(user("1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void contentAdminCanListCommentsAndMeetupReviews() throws Exception {
        mockMvc.perform(get("/api/v1/admin/post-comments")
                        .param("status", "HIDDEN_BY_ADMIN")
                        .with(user("1").roles("CONTENT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());

        mockMvc.perform(get("/api/v1/admin/meetup-reviews")
                        .param("status", "HIDDEN")
                        .param("rating", "1")
                        .with(user("1").roles("CONTENT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void rejectsInvalidPaginationAndRating() throws Exception {
        mockMvc.perform(get("/api/v1/admin/post-comments")
                        .param("size", "101")
                        .with(user("1").roles("CONTENT_ADMIN")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/meetup-reviews")
                        .param("rating", "6")
                        .with(user("1").roles("CONTENT_ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
