package com.mal.service;

import com.mal.integration.dto.*;
import com.mal.model.KycDecisionType;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DecisionEngine {

    private final MeterRegistry meterRegistry;
    private final int confidenceThreshold;

    public DecisionEngine(
            MeterRegistry meterRegistry,
            @Value("${kyc.decision.confidence-threshold:80}") int confidenceThreshold
    ) {
        this.meterRegistry = meterRegistry;
        this.confidenceThreshold = confidenceThreshold;
    }

    public KycDecisionType decide(
            DocumentVerificationResponse doc,
            BiometricResponse bio,
            AddressVerificationResponse addr,
            SanctionsResponse sanc) {

        log.info("decision_input doc={} bio={} addr={} sanc={}",
                summarize(doc), summarize(bio), summarize(addr), summarize(sanc));

        KycDecisionType result;
        String reason;

        if (sanc != null && sanc.status() == SanctionsStatus.HIT) {
            result = KycDecisionType.REJECTED;
            reason = "SANCTIONS_HIT";
        } else if (isFail(doc) || isFail(bio) || isFail(addr)) {
            result = KycDecisionType.MANUAL_REVIEW;
            reason = "FAIL";
        } else if (isManual(doc) || isManual(bio) || isManual(addr)) {
            result = KycDecisionType.MANUAL_REVIEW;
            reason = "MANUAL";
        } else if (!above(doc.confidence()) ||
                !above(bio.confidence()) ||
                !above(addr.confidence())) {
            result = KycDecisionType.MANUAL_REVIEW;
            reason = "LOW_CONFIDENCE";
        } else {
            result = KycDecisionType.APPROVED;
            reason = "OK";
        }

        log.info("decision_output result={} reason={}", result, reason);

        meterRegistry.counter("kyc.decision.total", "result", result.name()).increment();
        meterRegistry.counter("kyc.decision.reason", "reason", reason).increment();

        return result;
    }

    private boolean isFail(Object o) {
        if (o instanceof DocumentVerificationResponse d) return d.status() == VerificationStatus.FAIL;
        if (o instanceof BiometricResponse b) return b.status() == VerificationStatus.FAIL;
        if (o instanceof AddressVerificationResponse a) return a.status() == VerificationStatus.FAIL;
        return false;
    }

    private boolean isManual(Object o) {
        if (o instanceof DocumentVerificationResponse d) return d.status() == VerificationStatus.MANUAL_REVIEW;
        if (o instanceof BiometricResponse b) return b.status() == VerificationStatus.MANUAL_REVIEW;
        if (o instanceof AddressVerificationResponse a) return a.status() == VerificationStatus.MANUAL_REVIEW;
        return false;
    }

    private boolean above(int v) {
        return v >= confidenceThreshold;
    }

    private Object summarize(Object o) {
        return o == null ? "null" : o.toString();
    }
}
