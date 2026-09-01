package com.austin.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
class JacksonConfigTests {

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void serializesLocalDateTimeWithUnifiedFormat() throws Exception {
        String json = jsonMapper.writeValueAsString(LocalDateTime.of(2026, 9, 1, 12, 30, 45));

        assertThat(json).isEqualTo("\"2026-09-01 12:30:45\"");
        assertThat(jsonMapper.readValue(json, LocalDateTime.class))
                .isEqualTo(LocalDateTime.of(2026, 9, 1, 12, 30, 45));
    }

    @Test
    void usesShanghaiAsDefaultTimeZone() {
        String json = jsonMapper.writeValueAsString(OffsetDateTime.parse("2026-09-01T04:30:45Z"));

        assertThat(json).contains("2026-09-01T12:30:45+08:00");
    }
}
