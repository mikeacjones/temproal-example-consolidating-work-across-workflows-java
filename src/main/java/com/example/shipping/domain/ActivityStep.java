package com.example.shipping.domain;

public record ActivityStep(
        String itemWorkflowId,
        String itemId,
        String stepName,
        long syntheticDurationMillis) {}
