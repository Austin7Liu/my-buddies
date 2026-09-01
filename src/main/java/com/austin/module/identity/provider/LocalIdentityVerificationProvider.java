package com.austin.module.identity.provider;

import com.austin.module.identity.config.IdentityProperties;
import com.austin.module.identity.domain.Gender;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class LocalIdentityVerificationProvider implements IdentityVerificationProvider {

    private static final String PROVIDER = "LOCAL_RULES";
    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
    private final byte[] fingerprintSecret;

    public LocalIdentityVerificationProvider(IdentityProperties properties) {
        this.fingerprintSecret = properties.fingerprintSecret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public IdentityProviderResult verify(IdentityVerificationCommand command) {
        String name = command.realName().trim();
        String number = command.identityNumber().trim().toUpperCase();
        if (name.length() < 2 || !name.matches("[\\p{IsHan}·]{2,50}")) {
            return IdentityProviderResult.failed(PROVIDER, "INVALID_REAL_NAME_FORMAT");
        }
        if (!isValidIdentityNumber(number)) {
            return IdentityProviderResult.failed(PROVIDER, "INVALID_IDENTITY_NUMBER");
        }
        try {
            LocalDate birthDate = LocalDate.parse(number.substring(6, 14),
                    java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            Gender gender = ((number.charAt(16) - '0') % 2 == 0) ? Gender.FEMALE : Gender.MALE;
            String fingerprint = hmacSha256(number);
            return new IdentityProviderResult(true, fingerprint, birthDate, gender, PROVIDER,
                    "local-" + fingerprint.substring(0, 16), null);
        } catch (DateTimeParseException exception) {
            return IdentityProviderResult.failed(PROVIDER, "INVALID_BIRTH_DATE");
        }
    }

    private boolean isValidIdentityNumber(String number) {
        if (!number.matches("[1-9]\\d{16}[0-9X]")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (number.charAt(i) - '0') * WEIGHTS[i];
        }
        return CHECK_CODES[sum % 11] == number.charAt(17);
    }

    private String hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(fingerprintSecret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法生成实名主体指纹", exception);
        }
    }
}

