package com.example.shipping.activity;

import com.example.shipping.config.DemoTimingProperties;
import com.example.shipping.config.TemporalProperties;
import com.example.shipping.domain.ConsolidationRunInput;
import com.example.shipping.domain.RegistrationReceipt;
import com.example.shipping.domain.ShipmentItem;
import com.example.shipping.domain.WorkflowIdFactory;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

public class ConsolidationGatewayActivitiesImpl implements ConsolidationGatewayActivities {
    private final WorkflowClient client;
    private final TemporalProperties temporal;
    private final DemoTimingProperties timing;

    public ConsolidationGatewayActivitiesImpl(
            WorkflowClient client,
            TemporalProperties temporal,
            DemoTimingProperties timing) {
        this.client = client;
        this.temporal = temporal;
        this.timing = timing;
    }

    @Override
    public RegistrationReceipt submitItem(ShipmentItem item) {
        String workflowId = WorkflowIdFactory.consolidation(item.patientId(), item.addressId());
        WorkflowStub workflow = client.newUntypedWorkflowStub(
                "ConsolidationWorkflow",
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(temporal.getTaskQueue())
                        .setWorkflowIdReusePolicy(
                                WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
                        .build());

        ConsolidationRunInput firstRun = ConsolidationRunInput.firstRun(
                item.patientId(),
                item.addressId(),
                timing.getConsolidationWindow().toMillis(),
                timing.getShipmentActivityDuration().toMillis());

        WorkflowExecution execution = workflow.signalWithStart(
                "addItem",
                new Object[] {item},
                new Object[] {firstRun});
        return new RegistrationReceipt(execution.getWorkflowId(), execution.getRunId(), true);
    }
}
