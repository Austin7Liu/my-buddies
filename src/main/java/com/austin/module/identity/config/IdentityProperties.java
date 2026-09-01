package com.austin.module.identity.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.identity")
public record IdentityProperties(
        @NotBlank @Size(min = 32, message = "实名主体指纹密钥至少需要 32 个字符") String fingerprintSecret,
        int minimumAge) {

    public IdentityProperties {
        if (minimumAge < 18) {
            throw new IllegalArgumentException("实名用户最低年龄不能小于 18 岁");
        }
    }
}

