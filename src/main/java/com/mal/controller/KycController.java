package com.mal.controller;

import com.mal.model.KycDecision;
import com.mal.model.VerificationRequest;
import com.mal.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/kyc")
public class KycController {

    private final KycService kycService;

    @PostMapping("/verify")
    public KycDecision verify(@RequestBody VerificationRequest request) {
        return kycService.verify(request);
    }
}
