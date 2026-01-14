package com.mal.integration.dto;

public record AddressVerificationRequest(
        String customerId,
        String address,
        String proofType,
        String proofDate,
        String proofUrl
) {
}
