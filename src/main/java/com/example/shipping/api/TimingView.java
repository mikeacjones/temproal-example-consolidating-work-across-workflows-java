package com.example.shipping.api;

public record TimingView(
        String consolidationWindow,
        long consolidationWindowMillis,
        String shipmentActivityDuration,
        long shipmentActivityDurationMillis,
        String itemStepDuration,
        long itemStepDurationMillis) {}
