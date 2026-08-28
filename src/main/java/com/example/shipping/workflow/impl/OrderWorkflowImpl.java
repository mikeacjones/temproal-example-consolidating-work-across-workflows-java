package com.example.shipping.workflow.impl;

import com.example.shipping.domain.ItemBehavior;
import com.example.shipping.domain.ItemInput;
import com.example.shipping.domain.ItemResult;
import com.example.shipping.domain.OrderInput;
import com.example.shipping.domain.OrderWorkflowStatus;
import com.example.shipping.workflow.ConcurrentActivitiesItemWorkflow;
import com.example.shipping.workflow.NoFinalActivitiesItemWorkflow;
import com.example.shipping.workflow.OrderWorkflow;
import com.example.shipping.workflow.PostShipmentActivitiesItemWorkflow;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.util.ArrayList;
import java.util.List;

public class OrderWorkflowImpl implements OrderWorkflow {
    private OrderInput input;
    private String state = "STARTING";
    private final List<ItemResult> completed = new ArrayList<>();

    @Override
    public List<ItemResult> run(OrderInput input) {
        this.input = input;
        state = "RUNNING_ITEM_WORKFLOWS";

        List<Promise<ItemResult>> children = new ArrayList<>();
        for (ItemInput item : input.items()) {
            children.add(startItem(item));
        }

        for (Promise<ItemResult> child : children) {
            completed.add(child.get());
        }
        state = "COMPLETED";
        return List.copyOf(completed);
    }

    private Promise<ItemResult> startItem(ItemInput item) {
        ChildWorkflowOptions options = ChildWorkflowOptions.newBuilder()
                .setWorkflowId(item.itemWorkflowId())
                .build();

        return switch (item.behavior()) {
            case NO_FINAL_ACTIVITIES -> {
                NoFinalActivitiesItemWorkflow child = Workflow.newChildWorkflowStub(
                        NoFinalActivitiesItemWorkflow.class, options);
                yield Async.function(child::run, item);
            }
            case FINAL_ACTIVITIES_AFTER_SHIPMENT -> {
                PostShipmentActivitiesItemWorkflow child = Workflow.newChildWorkflowStub(
                        PostShipmentActivitiesItemWorkflow.class, options);
                yield Async.function(child::run, item);
            }
            case CONCURRENT_ACTIVITIES_WHILE_SHIPPING -> {
                ConcurrentActivitiesItemWorkflow child = Workflow.newChildWorkflowStub(
                        ConcurrentActivitiesItemWorkflow.class, options);
                yield Async.function(child::run, item);
            }
        };
    }

    @Override
    public OrderWorkflowStatus status() {
        return new OrderWorkflowStatus(
                input == null ? null : input.orderId(),
                input == null ? null : input.patientId(),
                input == null ? null : input.addressId(),
                state,
                input == null ? 0 : input.items().size(),
                List.copyOf(completed));
    }
}
