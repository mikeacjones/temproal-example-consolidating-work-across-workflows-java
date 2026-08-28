package com.example.shipping.domain;

import java.util.List;

public record ConsolidationRunInput(
        String patientId,
        String addressId,
        long consolidationWindowMillis,
        long shipmentActivityDurationMillis,
        int batchNumber,
        List<ShipmentItem> pendingItems,
        Long firstPendingArrivalEpochMillis,
        List<String> seenSubmissionIds) {

    public static ConsolidationRunInput firstRun(
            String patientId,
            String addressId,
            long consolidationWindowMillis,
            long shipmentActivityDurationMillis) {
        return new ConsolidationRunInput(
                patientId,
                addressId,
                consolidationWindowMillis,
                shipmentActivityDurationMillis,
                1,
                List.of(),
                null,
                List.of());
    }
}
