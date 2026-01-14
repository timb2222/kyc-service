package com.mal.integration.dto;

import java.util.List;

public record DocumentVerificationResponse(
        VerificationStatus status,       // PASS | FAIL | MANUAL_REVIEW
        int confidence,      // 0-100
        List<String> reasons
) {
}
