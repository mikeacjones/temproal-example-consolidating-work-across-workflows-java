package com.example.shipping.domain;

public record RegistrationReceipt(
        String consolidationWorkflowId,
        String runId,
        boolean signalAccepted) {}
