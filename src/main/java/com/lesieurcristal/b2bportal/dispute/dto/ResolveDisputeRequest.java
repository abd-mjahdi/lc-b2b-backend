package com.lesieurcristal.b2bportal.dispute.dto;

import jakarta.validation.constraints.Size;

public record ResolveDisputeRequest(
        @Size(max = 4000) String resolutionNote
) {
}