package com.mal.integration.dto;

import java.util.List;

public record AddressVerificationResponse(
        VerificationStatus status,
        int confidence,
        List<String> reasons
) {
}
