package com.example.shipping.domain;

public record ItemInput(
        String orderId,
        String itemId,
        String itemWorkflowId,
        String patientId,
        String addressId,
        String consolidationWorkflowId,
        String sku,
        String description,
        ItemBehavior behavior,
        long itemStepDurationMillis) {}
