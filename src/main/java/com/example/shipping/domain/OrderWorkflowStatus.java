package com.example.shipping.domain;

import java.util.List;

public record OrderWorkflowStatus(
        String orderId,
        String patientId,
        String addressId,
        String state,
        int totalItems,
        List<ItemResult> completedItems) {}
