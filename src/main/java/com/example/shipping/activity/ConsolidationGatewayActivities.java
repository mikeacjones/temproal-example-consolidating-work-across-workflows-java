package com.example.shipping.activity;

import com.example.shipping.domain.RegistrationReceipt;
import com.example.shipping.domain.ShipmentItem;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ConsolidationGatewayActivities {
    @ActivityMethod
    RegistrationReceipt submitItem(ShipmentItem item);
}
