package com.example.shipping.workflow;

import com.example.shipping.domain.ItemWorkflowStatus;
import com.example.shipping.domain.ShipmentResult;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;

public interface ItemWorkflowMessages {
    @SignalMethod(name = "approve")
    void approve();

    @SignalMethod(name = "shipmentCompleted")
    void shipmentCompleted(ShipmentResult result);

    @QueryMethod(name = "status")
    ItemWorkflowStatus status();
}
