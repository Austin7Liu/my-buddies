package com.austin.config;

import java.time.ZoneId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Shanghai");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> {
            SimpleModule javaTimeFormatModule = new SimpleModule("my-partner-java-time-format");
            javaTimeFormatModule.addSerializer(LocalDate.class, new LocalDateSerializer(DATE_FORMATTER));
            javaTimeFormatModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER));
            javaTimeFormatModule.addSerializer(LocalTime.class, new LocalTimeSerializer(TIME_FORMATTER));
            javaTimeFormatModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMATTER));
            javaTimeFormatModule.addSerializer(
                    LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
            javaTimeFormatModule.addDeserializer(
                    LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));

            builder.defaultTimeZone(TimeZone.getTimeZone(DEFAULT_ZONE_ID));
            builder.addModule(javaTimeFormatModule);
        };
    }
}
