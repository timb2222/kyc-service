package com.mal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mal.integration.AddressVerificationClient;
import com.mal.integration.BiometricClient;
import com.mal.integration.DocumentVerificationClient;
import com.mal.integration.SanctionsClient;
import com.mal.integration.dto.*;
import com.mal.model.KycDecisionType;
import com.mal.model.VerificationRequest;
import com.mal.service.DecisionEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KycControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    DocumentVerificationClient documentClient;
    @MockBean
    BiometricClient biometricClient;
    @MockBean
    AddressVerificationClient addressClient;
    @MockBean
    SanctionsClient sanctionsClient;
    @MockBean
    DecisionEngine decisionEngine;

    @Test
    void verify_shouldReturnApproved_whenDecisionEngineApproves() throws Exception {
        // Given
        var doc = new DocumentVerificationResponse(VerificationStatus.PASS, 95, null);
        var bio = new BiometricResponse(VerificationStatus.PASS, 95, 0.98);
        var addr = new AddressVerificationResponse(VerificationStatus.PASS, 95, null);
        var sanc = new SanctionsResponse(SanctionsStatus.CLEAR, 0, null);

        when(documentClient.verify(any())).thenReturn(doc);
        when(biometricClient.match(any())).thenReturn(bio);
        when(addressClient.verify(any())).thenReturn(addr);
        when(sanctionsClient.check(any())).thenReturn(sanc);
        when(decisionEngine.decide(doc, bio, addr, sanc))
                .thenReturn(KycDecisionType.APPROVED);

        var request = new VerificationRequest(
                "req-1",
                "cust-1",
                "John Doe",
                "1990-01-01",
                "UK",
                "PASSPORT",
                "P123456",
                "2030-01-01",
                "http://img-doc",
                "http://img-selfie",
                "http://img-id",
                "Baker Street 221B",
                "UTILITY",
                "2024-01-01",
                "http://proof"
        );

        // When + Then
        mockMvc.perform(post("/api/v1/kyc/verify")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andExpect(jsonPath("$.document.status").value("PASS"))
                .andExpect(jsonPath("$.biometric.status").value("PASS"))
                .andExpect(jsonPath("$.address.status").value("PASS"))
                .andExpect(jsonPath("$.sanctions.status").value("CLEAR"));
    }
}
