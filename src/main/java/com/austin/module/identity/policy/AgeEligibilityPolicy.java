package com.austin.module.identity.policy;

import com.austin.module.identity.config.IdentityProperties;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AgeEligibilityPolicy {
    private final IdentityProperties properties;
    private final Clock clock;

    public boolean isAdult(LocalDate birthDate) {
        return birthDate != null
                && !birthDate.isAfter(LocalDate.now(clock))
                && Period.between(birthDate, LocalDate.now(clock)).getYears() >= properties.minimumAge();
    }
}

