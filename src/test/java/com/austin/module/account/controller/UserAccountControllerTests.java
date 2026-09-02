package com.austin.module.account.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserAccountControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountService userAccountService;

    @Test
    void returnsOnlyCurrentAuthenticatedAccountWithoutExposingPhone() throws Exception {
        UserAccount account = userAccountService.create("13900139000");

        mockMvc.perform(get("/api/v1/accounts/{accountId}", account.getId())
                        .with(user(Long.toString(account.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maskedPhone").value("139****9000"))
                .andExpect(jsonPath("$.data.phone").doesNotExist())
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
    }

    @Test
    void rejectsInvalidPhoneBeforeSendingSms() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sms-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void missingJsonContentTypeReturnsUnsupportedMediaTypeInsteadOfUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sms-codes")
                        .content("{\"phone\":\"13900139000\"}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void rejectsUnauthenticatedAccountAccess() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAccessToAnotherAccount() throws Exception {
        UserAccount account = userAccountService.create("13900139001");

        mockMvc.perform(get("/api/v1/accounts/{accountId}", account.getId())
                        .with(user("999")))
                .andExpect(status().isForbidden());
    }
}
