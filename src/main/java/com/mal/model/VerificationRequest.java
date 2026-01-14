package com.mal.model;

public record VerificationRequest(
        String requestId,
        String customerId,
        String fullName,
        String dateOfBirth,
        String nationality,
        String documentType,
        String documentNumber,
        String documentExpiry,
        String documentImageUrl,
        String selfieUrl,
        String idPhotoUrl,
        String address,
        String proofType,
        String proofDate,
        String proofUrl
) {
}
