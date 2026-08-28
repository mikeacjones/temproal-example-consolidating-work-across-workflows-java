package com.example.shipping.domain;

import java.util.List;

public record OrderInput(
        String orderId,
        String patientId,
        String addressId,
        String consolidationWorkflowId,
        long itemStepDurationMillis,
        List<ItemInput> items) {}
