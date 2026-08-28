package com.example.shipping.domain;

public record ShipmentItem(
        String submissionId,
        String patientId,
        String addressId,
        String orderId,
        String itemId,
        String itemWorkflowId,
        String sku,
        String description) {}
