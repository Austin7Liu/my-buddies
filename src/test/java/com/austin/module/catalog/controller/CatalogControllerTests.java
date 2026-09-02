package com.austin.module.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.catalog.domain.CatalogAdminAuditLog;
import com.austin.module.catalog.mapper.CatalogAdminAuditLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountService accountService;
    @Autowired private CatalogAdminAuditLogMapper auditMapper;
    private UserAccount operator;

    @BeforeEach
    void setUp() { operator = accountService.create("13900139300"); }

    @Test
    void anonymousUserCanListSeededPrimaryCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10))
                .andExpect(jsonPath("$.data[0].code").value("sports"))
                .andExpect(jsonPath("$.data[0].name").value("运动"));
    }

    @Test
    void contentAdminCanCreateTopicAndOperationIsAudited() throws Exception {
        mockMvc.perform(post("/api/v1/admin/catalog/categories/101/topics")
                        .with(user(operator.getId().toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"tennis","name":"网球","description":"网球活动","sortOrder":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryId").value(101))
                .andExpect(jsonPath("$.data.code").value("tennis"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        assertThat(auditMapper.selectCount(new LambdaQueryWrapper<CatalogAdminAuditLog>()
                .eq(CatalogAdminAuditLog::getOperatorAccountId, operator.getId()))).isEqualTo(1);
        mockMvc.perform(get("/api/v1/categories/101/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("tennis"));
    }

    @Test
    void ordinaryUserCannotMaintainCatalog() throws Exception {
        mockMvc.perform(post("/api/v1/admin/catalog/categories")
                        .with(user(operator.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"new-category","name":"新分类","sortOrder":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledCategoryIsHiddenFromPublicButVisibleToAdmin() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/catalog/categories/101/enabled")
                        .with(user(operator.getId().toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(9));
        mockMvc.perform(get("/api/v1/admin/catalog/categories")
                        .with(user(operator.getId().toString()).roles("CONTENT_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10));
    }

    @Test
    void duplicateTopicCodeReturnsConflict() throws Exception {
        String body = "{\"code\":\"running\",\"name\":\"跑步\",\"sortOrder\":10}";
        mockMvc.perform(post("/api/v1/admin/catalog/categories/101/topics")
                        .with(user(operator.getId().toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/catalog/categories/104/topics")
                        .with(user(operator.getId().toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
}

