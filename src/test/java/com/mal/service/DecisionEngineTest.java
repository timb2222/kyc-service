package com.mal.service;

import com.mal.integration.dto.*;
import com.mal.model.KycDecisionType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DecisionEngineTest {

    private MeterRegistry meterRegistry() {
        MeterRegistry registry = mock(MeterRegistry.class);
        when(registry.counter(anyString(), anyString(), anyString()))
                .thenReturn(mock(Counter.class));
        return registry;
    }

    private DecisionEngine engine() {
        return new DecisionEngine(meterRegistry(), 80);
    }

    private DocumentVerificationResponse doc(VerificationStatus s, int conf) {
        return new DocumentVerificationResponse(s, conf, null);
    }

    private BiometricResponse bio(VerificationStatus s, int conf) {
        return new BiometricResponse(s, conf, 0.0);
    }

    private AddressVerificationResponse addr(VerificationStatus s, int conf) {
        return new AddressVerificationResponse(s, conf, null);
    }

    private SanctionsResponse sanc(SanctionsStatus s) {
        return new SanctionsResponse(s, 0, null);
    }

    @Test
    void decide_shouldReturnRejected_whenSanctionsHit() {
        // given
        var engine = engine();

        // when
        var result = engine.decide(
                doc(VerificationStatus.PASS, 90),
                bio(VerificationStatus.PASS, 90),
                addr(VerificationStatus.PASS, 90),
                sanc(SanctionsStatus.HIT)
        );

        // then
        assertEquals(KycDecisionType.REJECTED, result);
    }

    @Test
    void decide_shouldReturnManual_whenAnyFail() {
        // given
        var engine = engine();

        // when
        var result = engine.decide(
                doc(VerificationStatus.FAIL, 0),
                bio(VerificationStatus.PASS, 90),
                addr(VerificationStatus.PASS, 90),
                sanc(SanctionsStatus.CLEAR)
        );

        // then
        assertEquals(KycDecisionType.MANUAL_REVIEW, result);
    }

    @Test
    void decide_shouldReturnManual_whenAnyManual() {
        // given
        var engine = engine();

        // when
        var result = engine.decide(
                doc(VerificationStatus.MANUAL_REVIEW, 90),
                bio(VerificationStatus.PASS, 90),
                addr(VerificationStatus.PASS, 90),
                sanc(SanctionsStatus.CLEAR)
        );

        // then
        assertEquals(KycDecisionType.MANUAL_REVIEW, result);
    }

    @Test
    void decide_shouldReturnManual_whenConfidenceBelowThreshold() {
        // given
        var engine = engine();

        // when
        var result = engine.decide(
                doc(VerificationStatus.PASS, 79),
                bio(VerificationStatus.PASS, 90),
                addr(VerificationStatus.PASS, 90),
                sanc(SanctionsStatus.CLEAR)
        );

        // then
        assertEquals(KycDecisionType.MANUAL_REVIEW, result);
    }

    @Test
    void decide_shouldReturnApproved_whenAllClearAndAboveThreshold() {
        // given
        var engine = engine();

        // when
        var result = engine.decide(
                doc(VerificationStatus.PASS, 95),
                bio(VerificationStatus.PASS, 90),
                addr(VerificationStatus.PASS, 88),
                sanc(SanctionsStatus.CLEAR)
        );

        // then
        assertEquals(KycDecisionType.APPROVED, result);
    }
}
