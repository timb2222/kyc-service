package com.mal.integration.dto;

public record BiometricRequest(
        String customerId,
        String selfieUrl,
        String idPhotoUrl
) {
}
