package com.example.shipping.workflow;

import com.example.shipping.domain.ItemResult;
import com.example.shipping.domain.OrderInput;
import com.example.shipping.domain.OrderWorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;

@WorkflowInterface
public interface OrderWorkflow {
    @WorkflowMethod(name = "OrderWorkflow")
    List<ItemResult> run(OrderInput input);

    @QueryMethod(name = "status")
    OrderWorkflowStatus status();
}
