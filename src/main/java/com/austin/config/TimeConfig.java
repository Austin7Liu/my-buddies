package com.austin.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TimeConfig {

    @Bean
    public Clock applicationClock() {
        return Clock.system(JacksonConfig.DEFAULT_ZONE_ID);
    }
}
