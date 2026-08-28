package com.example.shipping.activity;

import com.example.shipping.domain.ShipmentBatch;
import com.example.shipping.domain.ShipmentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShipmentActivitiesImpl implements ShipmentActivities {
    private static final Logger log = LoggerFactory.getLogger(ShipmentActivitiesImpl.class);

    @Override
    public ShipmentResult consolidateAndShip(ShipmentBatch batch) {
        log.info(
                "Starting synthetic shipment {} with {} items for patient {} / address {}",
                batch.batchId(),
                batch.items().size(),
                batch.patientId(),
                batch.addressId());

        SyntheticWait.withHeartbeats(batch.syntheticDurationMillis(), batch.batchId());

        String tracking = "DEMO-" + batch.patientId().toUpperCase() + "-" + batch.batchNumber();
        log.info("Synthetic shipment {} completed as {}", batch.batchId(), tracking);
        return new ShipmentResult(
                batch.batchId(),
                batch.batchNumber(),
                true,
                tracking,
                "Consolidated " + batch.items().size() + " item(s) and shipped successfully");
    }
}
