package com.example.shipping.workflow;

import com.example.shipping.domain.ItemInput;
import com.example.shipping.domain.ItemResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ConcurrentActivitiesItemWorkflow extends ItemWorkflowMessages {
    @WorkflowMethod(name = "ConcurrentActivitiesItemWorkflow")
    ItemResult run(ItemInput input);
}
