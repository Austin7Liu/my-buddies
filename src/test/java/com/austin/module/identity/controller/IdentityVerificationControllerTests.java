package com.austin.module.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.identity.domain.IdentityVerification;
import com.austin.module.identity.mapper.IdentityVerificationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
class IdentityVerificationControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountService userAccountService;
    @Autowired private IdentityVerificationMapper identityVerificationMapper;

    @Test
    void returnsUnverifiedWithoutCreatingDatabaseRow() throws Exception {
        UserAccount account = userAccountService.create("13900139100");

        mockMvc.perform(get("/api/v1/identity-verification/me")
                        .with(user(account.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNVERIFIED"))
                .andExpect(jsonPath("$.data.adult").value(false));

        assertThat(findByAccountId(account.getId())).isNull();
    }

    @Test
    void verifiesAdultWithoutReturningOrPersistingPlaintextIdentity() throws Exception {
        UserAccount account = userAccountService.create("13900139101");

        mockMvc.perform(post("/api/v1/identity-verification")
                        .with(user(account.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"realName":"测试用户","identityNumber":"11010519491231002X"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.birthDate").value("1949-12-31"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.adult").value(true))
                .andExpect(jsonPath("$.data.realName").doesNotExist())
                .andExpect(jsonPath("$.data.identityNumber").doesNotExist())
                .andExpect(jsonPath("$.data.subjectFingerprint").doesNotExist())
                .andExpect(jsonPath("$.data.providerReference").doesNotExist());

        IdentityVerification stored = findByAccountId(account.getId());
        assertThat(stored.getSubjectFingerprint()).hasSize(64)
                .doesNotContain("11010519491231002X");
    }

    @Test
    void rejectsBindingSameIdentitySubjectToAnotherAccount() throws Exception {
        UserAccount first = userAccountService.create("13900139102");
        UserAccount second = userAccountService.create("13900139103");
        String body = "{\"realName\":\"测试用户\",\"identityNumber\":\"11010519491231002X\"}";

        mockMvc.perform(post("/api/v1/identity-verification")
                        .with(user(first.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/identity-verification")
                        .with(user(second.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("该实名主体已绑定其他账户"));
    }

    @Test
    void recordsFailedResultWithoutDerivedIdentityAttributes() throws Exception {
        UserAccount account = userAccountService.create("13900139104");

        mockMvc.perform(post("/api/v1/identity-verification")
                        .with(user(account.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"realName":"测试用户","identityNumber":"110105194912310020"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failureCode").value("INVALID_IDENTITY_NUMBER"))
                .andExpect(jsonPath("$.data.birthDate").doesNotExist())
                .andExpect(jsonPath("$.data.gender").doesNotExist());

        IdentityVerification stored = findByAccountId(account.getId());
        assertThat(stored.getSubjectFingerprint()).isNull();
        assertThat(stored.getBirthDate()).isNull();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/identity-verification/me"))
                .andExpect(status().isUnauthorized());
    }

    private IdentityVerification findByAccountId(long accountId) {
        return identityVerificationMapper.selectOne(new LambdaQueryWrapper<IdentityVerification>()
                .eq(IdentityVerification::getAccountId, accountId));
    }
}

