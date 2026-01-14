package com.mal.integration.dto;

public record BiometricResponse(
        VerificationStatus status,
        int confidence,
        double similarityScore
) {
}
