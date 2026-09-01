package com.austin;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MyPartnerApplicationTests {

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void contextLoads() {
        assertThat(mybatisPlusInterceptor).isNotNull();
        assertThat(securityFilterChain).isNotNull();
    }

}
