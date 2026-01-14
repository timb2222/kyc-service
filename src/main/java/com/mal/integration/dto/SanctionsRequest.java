package com.mal.integration.dto;

public record SanctionsRequest(
        String customerId,
        String fullName,
        String dateOfBirth,
        String nationality
) {
}
