package com.example.shipping.domain;

public record ShipmentResult(
        String batchId,
        int batchNumber,
        boolean successful,
        String trackingNumber,
        String message) {}
