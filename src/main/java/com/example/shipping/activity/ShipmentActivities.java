package com.example.shipping.activity;

import com.example.shipping.domain.ShipmentBatch;
import com.example.shipping.domain.ShipmentResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ShipmentActivities {
    @ActivityMethod
    ShipmentResult consolidateAndShip(ShipmentBatch batch);
}
