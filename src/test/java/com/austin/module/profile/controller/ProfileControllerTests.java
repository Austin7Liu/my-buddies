package com.austin.module.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.identity.service.IdentityVerificationService;
import com.austin.module.profile.domain.AvatarCode;
import com.austin.module.profile.domain.UserProfile;
import com.austin.module.profile.mapper.UserProfileMapper;
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
class ProfileControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountService accountService;

    @Autowired
    private IdentityVerificationService identityService;

    @Autowired
    private UserProfileMapper profileMapper;

    @Test
    void publicProfileUsesSafeDefaultsWithoutPersistingSensitiveData() throws Exception {
        UserAccount account = accountService.create("13900139700");

        mockMvc.perform(get("/api/v1/profiles/{accountId}", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(account.getId()))
                .andExpect(jsonPath("$.data.nickname").isNotEmpty())
                .andExpect(jsonPath("$.data.avatarCode").value("PANDA"))
                .andExpect(jsonPath("$.data.verified").value(false))
                .andExpect(jsonPath("$.data.phone").doesNotExist())
                .andExpect(jsonPath("$.data.realName").doesNotExist())
                .andExpect(jsonPath("$.data.birthDate").doesNotExist());

        assertThat(profileMapper.selectById(account.getId())).isNull();
    }

    @Test
    void ownerCanUpdateProfileAndPublicResponseContainsOnlySafeIdentitySummary() throws Exception {
        UserAccount account = accountService.create("13900139701");
        identityService.submit(account.getId(), "测试用户", "110105198806150016");

        mockMvc.perform(put("/api/v1/profiles/me")
                        .with(user(account.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "  网球熊猫  ",
                                  "avatarCode": "FOX",
                                  "bio": "周末网球和主机游戏玩家",
                                  "city": "杭州",
                                  "district": "滨江区"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("网球熊猫"))
                .andExpect(jsonPath("$.data.avatarCode").value("FOX"))
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.age").value(38))
                .andExpect(jsonPath("$.data.phone").doesNotExist())
                .andExpect(jsonPath("$.data.birthDate").doesNotExist());

        UserProfile stored = profileMapper.selectById(account.getId());
        assertThat(stored.getNickname()).isEqualTo("网球熊猫");
        assertThat(stored.getAvatarCode()).isEqualTo(AvatarCode.FOX);

        mockMvc.perform(get("/api/v1/profiles/{accountId}", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("网球熊猫"))
                .andExpect(jsonPath("$.data.city").value("杭州"))
                .andExpect(jsonPath("$.data.verified").value(true));
    }

    @Test
    void profileUpdateRequiresAuthenticationAndValidInput() throws Exception {
        mockMvc.perform(put("/api/v1/profiles/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"玩家\",\"avatarCode\":\"CAT\"}"))
                .andExpect(status().isUnauthorized());

        UserAccount account = accountService.create("13900139702");
        mockMvc.perform(put("/api/v1/profiles/me")
                        .with(user(account.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"A\",\"avatarCode\":\"CAT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void myProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me"))
                .andExpect(status().isUnauthorized());
    }
}
