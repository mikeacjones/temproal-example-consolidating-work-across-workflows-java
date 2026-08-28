package com.example.shipping.workflow;

import com.example.shipping.domain.ConsolidationRunInput;
import com.example.shipping.domain.ConsolidationWorkflowStatus;
import com.example.shipping.domain.ShipmentItem;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ConsolidationWorkflow {
    @WorkflowMethod(name = "ConsolidationWorkflow")
    void run(ConsolidationRunInput input);

    @SignalMethod(name = "addItem")
    void addItem(ShipmentItem item);

    @QueryMethod(name = "status")
    ConsolidationWorkflowStatus status();
}
