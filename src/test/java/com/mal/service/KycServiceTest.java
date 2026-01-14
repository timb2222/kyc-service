package com.mal.service;

import com.mal.integration.AddressVerificationClient;
import com.mal.integration.BiometricClient;
import com.mal.integration.DocumentVerificationClient;
import com.mal.integration.SanctionsClient;
import com.mal.integration.dto.*;
import com.mal.model.KycDecision;
import com.mal.model.KycDecisionType;
import com.mal.model.VerificationRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class KycServiceTest {

    @Test
    void verify_shouldReturnDecisionAndCallDependencies() {
        // Given
        var documentClient = mock(DocumentVerificationClient.class);
        var biometricClient = mock(BiometricClient.class);
        var addressClient = mock(AddressVerificationClient.class);
        var sanctionsClient = mock(SanctionsClient.class);
        var decisionEngine = mock(DecisionEngine.class);
        var clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);

        var service = new KycService(
                documentClient,
                biometricClient,
                addressClient,
                sanctionsClient,
                decisionEngine,
                new SimpleMeterRegistry(),
                clock
        );

        var request = new VerificationRequest(
                "r1", "c1", "John Doe", "1990-01-01", "UK",
                "PASSPORT", "1234", "2030-01-01", "img://doc",
                "img://selfie", "img://id",
                "London", "UTILITY", "2025-01-01", "img://proof"
        );

        var doc = new DocumentVerificationResponse(VerificationStatus.PASS, 90, null);
        var bio = new BiometricResponse(VerificationStatus.PASS, 88, 0.95);
        var addr = new AddressVerificationResponse(VerificationStatus.PASS, 85, null);
        var sanc = new SanctionsResponse(SanctionsStatus.CLEAR, 0, null);

        when(documentClient.verify(any())).thenReturn(doc);
        when(biometricClient.match(any())).thenReturn(bio);
        when(addressClient.verify(any())).thenReturn(addr);
        when(sanctionsClient.check(any())).thenReturn(sanc);
        when(decisionEngine.decide(doc, bio, addr, sanc)).thenReturn(KycDecisionType.APPROVED);

        // When
        KycDecision result = service.verify(request);

        // Then
        verify(documentClient).verify(any());
        verify(biometricClient).match(any());
        verify(addressClient).verify(any());
        verify(sanctionsClient).check(any());
        verify(decisionEngine).decide(doc, bio, addr, sanc);

        assertEquals(KycDecisionType.APPROVED, result.decision());
        assertEquals(doc, result.document());
        assertEquals(bio, result.biometric());
        assertEquals(addr, result.address());
        assertEquals(sanc, result.sanctions());
        assertEquals("2026-01-01T10:00:00Z", result.timestamp());
    }
}
