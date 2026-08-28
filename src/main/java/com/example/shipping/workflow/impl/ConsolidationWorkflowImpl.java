package com.example.shipping.workflow.impl;

import com.example.shipping.activity.ShipmentActivities;
import com.example.shipping.domain.ConsolidationRunInput;
import com.example.shipping.domain.ConsolidationWorkflowStatus;
import com.example.shipping.domain.ShipmentBatch;
import com.example.shipping.domain.ShipmentItem;
import com.example.shipping.domain.ShipmentResult;
import com.example.shipping.workflow.ConsolidationWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.ExternalWorkflowStub;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConsolidationWorkflowImpl implements ConsolidationWorkflow {
    private final ShipmentActivities shipmentActivities = Workflow.newActivityStub(
            ShipmentActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofDays(2))
                    .setHeartbeatTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                    .build());

    private final String workflowId;
    private final String patientId;
    private final String addressId;
    private final long consolidationWindowMillis;
    private final long shipmentActivityDurationMillis;
    private final int batchNumber;
    private final Map<String, ShipmentItem> currentItems = new LinkedHashMap<>();
    private final Map<String, ShipmentItem> nextItems = new LinkedHashMap<>();
    private final Set<String> seenSubmissionIds = new LinkedHashSet<>();
    private Long firstCurrentArrival;
    private Long firstNextArrival;
    private ShipmentResult lastShipment;
    private String phase = "COLLECTING";

    @WorkflowInit
    public ConsolidationWorkflowImpl(ConsolidationRunInput input) {
        workflowId = Workflow.getInfo().getWorkflowId();
        patientId = input.patientId();
        addressId = input.addressId();
        consolidationWindowMillis = input.consolidationWindowMillis();
        shipmentActivityDurationMillis = input.shipmentActivityDurationMillis();
        batchNumber = input.batchNumber();
        if (input.pendingItems() != null) {
            input.pendingItems().forEach(item -> currentItems.put(item.submissionId(), item));
        }
        if (input.seenSubmissionIds() != null) {
            seenSubmissionIds.addAll(input.seenSubmissionIds());
        }
        firstCurrentArrival = input.firstPendingArrivalEpochMillis();
    }

    @Override
    public void run(ConsolidationRunInput ignored) {
        Workflow.await(() -> !currentItems.isEmpty());
        if (firstCurrentArrival == null) {
            firstCurrentArrival = Workflow.currentTimeMillis();
        }

        phase = "COLLECTING";
        long deadline = firstCurrentArrival + consolidationWindowMillis;
        long remaining = Math.max(0, deadline - Workflow.currentTimeMillis());
        Workflow.sleep(Duration.ofMillis(remaining));

        phase = "SHIPPING";
        List<ShipmentItem> batchItems = List.copyOf(currentItems.values());
        ShipmentBatch batch = new ShipmentBatch(
                workflowId + "-batch-" + batchNumber,
                patientId,
                addressId,
                batchNumber,
                shipmentActivityDurationMillis,
                batchItems);

        lastShipment = shipmentActivities.consolidateAndShip(batch);
        if (!lastShipment.successful()) {
            phase = "SHIPMENT_FAILED";
            throw ApplicationFailure.newNonRetryableFailure(
                    lastShipment.message(), "ConsolidatedShipmentFailed");
        }

        phase = "NOTIFYING_ITEMS";
        for (ShipmentItem item : batchItems) {
            ExternalWorkflowStub itemWorkflow =
                    Workflow.newUntypedExternalWorkflowStub(item.itemWorkflowId());
            itemWorkflow.signal("shipmentCompleted", lastShipment);
        }

        phase = "DRAINING_HANDLERS";
        Workflow.await(Workflow::isEveryHandlerFinished);
        continueIfItemsArrivedDuringShipment();

        phase = "COMPLETED";
        Workflow.await(Workflow::isEveryHandlerFinished);
        continueIfItemsArrivedDuringShipment();
    }

    private void continueIfItemsArrivedDuringShipment() {
        if (nextItems.isEmpty()) {
            return;
        }

        phase = "CONTINUING_AS_NEW";
        ConsolidationRunInput nextRun = new ConsolidationRunInput(
                patientId,
                addressId,
                consolidationWindowMillis,
                shipmentActivityDurationMillis,
                batchNumber + 1,
                List.copyOf(nextItems.values()),
                firstNextArrival,
                List.copyOf(seenSubmissionIds));
        Workflow.continueAsNew(nextRun);
    }

    @Override
    public void addItem(ShipmentItem item) {
        if (!isValid(item)) {
            Workflow.getLogger(ConsolidationWorkflowImpl.class)
                    .warn("Ignoring invalid consolidation item signal for workflow {}", workflowId);
            return;
        }
        if (!seenSubmissionIds.add(item.submissionId())) {
            return;
        }

        Map<String, ShipmentItem> destination = phase.equals("COLLECTING") ? currentItems : nextItems;
        boolean nextBatch = destination == nextItems;
        Long firstArrival = nextBatch ? firstNextArrival : firstCurrentArrival;

        if (!destination.containsKey(item.submissionId())) {
            destination.put(item.submissionId(), item);
            if (firstArrival == null) {
                firstArrival = Workflow.currentTimeMillis();
                if (nextBatch) {
                    firstNextArrival = firstArrival;
                } else {
                    firstCurrentArrival = firstArrival;
                }
            }
        }
    }

    private boolean isValid(ShipmentItem item) {
        return item != null
                && item.submissionId() != null
                && !item.submissionId().isBlank()
                && item.itemWorkflowId() != null
                && !item.itemWorkflowId().isBlank()
                && patientId.equals(item.patientId())
                && addressId.equals(item.addressId());
    }

    @Override
    public ConsolidationWorkflowStatus status() {
        Long deadline = firstCurrentArrival == null
                ? null
                : firstCurrentArrival + consolidationWindowMillis;
        return new ConsolidationWorkflowStatus(
                workflowId,
                patientId,
                addressId,
                batchNumber,
                batchNumber,
                phase,
                firstCurrentArrival,
                deadline,
                new ArrayList<>(currentItems.values()),
                new ArrayList<>(nextItems.values()),
                lastShipment);
    }
}
