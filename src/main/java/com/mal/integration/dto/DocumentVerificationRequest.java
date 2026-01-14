package com.mal.integration.dto;

public record DocumentVerificationRequest(
        String customerId,
        String documentType,
        String documentNumber,
        String expiryDate,
        String documentImageUrl
) {
}
