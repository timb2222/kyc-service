package com.mal.model;

import com.mal.integration.dto.AddressVerificationResponse;
import com.mal.integration.dto.BiometricResponse;
import com.mal.integration.dto.DocumentVerificationResponse;
import com.mal.integration.dto.SanctionsResponse;

public record KycDecision(
        KycDecisionType decision,
        DocumentVerificationResponse document,
        BiometricResponse biometric,
        AddressVerificationResponse address,
        SanctionsResponse sanctions,
        String timestamp
) {
}
