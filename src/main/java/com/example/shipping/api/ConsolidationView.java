package com.example.shipping.api;

import com.example.shipping.domain.ConsolidationWorkflowStatus;

public record ConsolidationView(
        String workflowId,
        String patientId,
        String addressId,
        String temporalStatus,
        ConsolidationWorkflowStatus workflow) {}
