package com.example.shipping.workflow.impl;

import com.example.shipping.activity.ConsolidationGatewayActivities;
import com.example.shipping.activity.ItemActivities;
import com.example.shipping.domain.ActivityStep;
import com.example.shipping.domain.ItemInput;
import com.example.shipping.domain.ItemResult;
import com.example.shipping.domain.ItemWorkflowStatus;
import com.example.shipping.domain.RegistrationReceipt;
import com.example.shipping.domain.ShipmentItem;
import com.example.shipping.domain.ShipmentResult;
import com.example.shipping.workflow.ItemWorkflowMessages;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

abstract class AbstractItemWorkflow implements ItemWorkflowMessages {
    private final ItemActivities itemActivities = Workflow.newActivityStub(
            ItemActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofDays(2))
                    .setHeartbeatTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                    .build());

    private final ConsolidationGatewayActivities consolidationGateway = Workflow.newActivityStub(
            ConsolidationGatewayActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(5).build())
                    .build());

    protected ItemInput input;
    protected String state = "STARTING";
    protected boolean awaitingApproval;
    protected boolean approved;
    protected RegistrationReceipt registration;
    protected ShipmentResult shipment;
    protected final List<String> completedSteps = new ArrayList<>();

    protected void initialize(ItemInput itemInput) {
        this.input = itemInput;
        this.state = "PROCESSING";
    }

    protected void step(String name) {
        state = "ACTIVITY: " + name;
        itemActivities.performStep(new ActivityStep(
                input.itemWorkflowId(),
                input.itemId(),
                name,
                input.itemStepDurationMillis()));
        completedSteps.add(name);
    }

    protected void waitForApproval() {
        awaitingApproval = true;
        state = "WAITING_FOR_APPROVAL";
        Workflow.await(() -> approved);
        awaitingApproval = false;
        completedSteps.add("human approval received");
    }

    protected void submitForConsolidation() {
        state = "SUBMITTING_FOR_CONSOLIDATION";
        registration = consolidationGateway.submitItem(new ShipmentItem(
                input.itemWorkflowId(),
                input.patientId(),
                input.addressId(),
                input.orderId(),
                input.itemId(),
                input.itemWorkflowId(),
                input.sku(),
                input.description()));
        state = "WAITING_FOR_CONSOLIDATED_SHIPMENT";
    }

    protected void waitForShipment() {
        state = "WAITING_FOR_CONSOLIDATED_SHIPMENT";
        Workflow.await(() -> shipment != null);
        if (!shipment.successful()) {
            state = "SHIPMENT_FAILED";
            throw ApplicationFailure.newNonRetryableFailure(
                    shipment.message(), "ConsolidatedShipmentFailed");
        }
        completedSteps.add("shipment " + shipment.trackingNumber() + " completed");
    }

    protected ItemResult complete() {
        state = "COMPLETED";
        return new ItemResult(input.itemId(), input.itemWorkflowId(), state, shipment.trackingNumber());
    }

    @Override
    public void approve() {
        approved = true;
    }

    @Override
    public void shipmentCompleted(ShipmentResult result) {
        shipment = result;
    }

    @Override
    public ItemWorkflowStatus status() {
        return new ItemWorkflowStatus(
                input == null ? null : input.itemId(),
                input == null ? Workflow.getInfo().getWorkflowId() : input.itemWorkflowId(),
                input == null ? null : input.description(),
                input == null ? null : input.behavior(),
                state,
                awaitingApproval,
                approved,
                List.copyOf(completedSteps),
                registration,
                shipment);
    }
}
