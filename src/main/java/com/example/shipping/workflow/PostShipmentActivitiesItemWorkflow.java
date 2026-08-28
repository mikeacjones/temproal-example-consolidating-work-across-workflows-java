package com.example.shipping.workflow;

import com.example.shipping.domain.ItemInput;
import com.example.shipping.domain.ItemResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PostShipmentActivitiesItemWorkflow extends ItemWorkflowMessages {
    @WorkflowMethod(name = "PostShipmentActivitiesItemWorkflow")
    ItemResult run(ItemInput input);
}
