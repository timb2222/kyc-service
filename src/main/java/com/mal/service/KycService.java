package com.mal.service;

import com.mal.integration.AddressVerificationClient;
import com.mal.integration.BiometricClient;
import com.mal.integration.DocumentVerificationClient;
import com.mal.integration.SanctionsClient;
import com.mal.integration.dto.AddressVerificationRequest;
import com.mal.integration.dto.BiometricRequest;
import com.mal.integration.dto.DocumentVerificationRequest;
import com.mal.integration.dto.SanctionsRequest;
import com.mal.model.KycDecision;
import com.mal.model.KycDecisionType;
import com.mal.model.VerificationRequest;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycService {

    private final DocumentVerificationClient documentClient;
    private final BiometricClient biometricClient;
    private final AddressVerificationClient addressClient;
    private final SanctionsClient sanctionsClient;
    private final DecisionEngine decisionEngine;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public KycDecision verify(VerificationRequest request) {

        var timer = meterRegistry.timer(
                "kyc.verify.time",
                "customerId", request.customerId()
        );

        return timer.record(() -> doVerify(request));
    }

    private KycDecision doVerify(VerificationRequest request) {
        log.info("kyc_request customerId={} nationality={} documentType={}",
                request.customerId(), request.nationality(), request.documentType());
        var start = System.currentTimeMillis();

        var docFuture = CompletableFuture.supplyAsync(() -> documentClient.verify(buildDocumentPayload(request)));
        var bioFuture = CompletableFuture.supplyAsync(() -> biometricClient.match(buildBiometricPayload(request)));
        var addrFuture = CompletableFuture.supplyAsync(() -> addressClient.verify(buildAddressPayload(request)));
        var sancFuture = CompletableFuture.supplyAsync(() -> sanctionsClient.check(buildSanctionsPayload(request)));

        CompletableFuture.allOf(docFuture, bioFuture, addrFuture, sancFuture).join();

        var doc = safeGet(docFuture);
        var bio = safeGet(bioFuture);
        var addr = safeGet(addrFuture);
        var sanc = safeGet(sancFuture);

        var total = System.currentTimeMillis() - start;
        KycDecisionType decision = decisionEngine.decide(doc, bio, addr, sanc);
        log.info("kyc_decision customerId={} result={} totalMs={}",
                request.customerId(), decision, total);


        return new KycDecision(
                decision,
                doc,
                bio,
                addr,
                sanc,
                Instant.now(clock).toString()
        );
    }

    private <T> T safeGet(CompletableFuture<T> f) {
        try {
            return f.get();
        } catch (Exception e) {
            return null;
        }
    }

    private DocumentVerificationRequest buildDocumentPayload(VerificationRequest req) {
        return new DocumentVerificationRequest(
                req.customerId(),
                req.documentType(),
                req.documentNumber(),
                req.documentExpiry(),
                req.documentImageUrl()
        );
    }

    private BiometricRequest buildBiometricPayload(VerificationRequest req) {
        return new BiometricRequest(
                req.customerId(),
                req.selfieUrl(),
                req.idPhotoUrl()
        );
    }

    private AddressVerificationRequest buildAddressPayload(VerificationRequest req) {
        return new AddressVerificationRequest(
                req.customerId(),
                req.address(),
                req.proofType(),
                req.proofDate(),
                req.proofUrl()
        );
    }

    private SanctionsRequest buildSanctionsPayload(VerificationRequest req) {
        return new SanctionsRequest(
                req.customerId(),
                req.fullName(),
                req.dateOfBirth(),
                req.nationality()
        );
    }
}
