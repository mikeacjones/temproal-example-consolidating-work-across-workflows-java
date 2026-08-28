package com.example.shipping.domain;

import java.util.List;

public record ConsolidationWorkflowStatus(
        String workflowId,
        String patientId,
        String addressId,
        int runNumber,
        int batchNumber,
        String phase,
        Long firstArrivalEpochMillis,
        Long windowClosesEpochMillis,
        List<ShipmentItem> currentBatchItems,
        List<ShipmentItem> nextBatchItems,
        ShipmentResult lastShipment) {}
