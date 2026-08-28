package com.example.shipping.domain;

import java.util.List;

public record ShipmentBatch(
        String batchId,
        String patientId,
        String addressId,
        int batchNumber,
        long syntheticDurationMillis,
        List<ShipmentItem> items) {}
