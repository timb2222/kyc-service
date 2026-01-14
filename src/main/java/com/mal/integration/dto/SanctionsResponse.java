package com.mal.integration.dto;

import java.util.List;

public record SanctionsResponse(
        SanctionsStatus status,   // CLEAR | HIT
        int matchCount,
        List<Object> matches
) {
}
