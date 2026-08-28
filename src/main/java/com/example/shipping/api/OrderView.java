package com.example.shipping.api;

import com.example.shipping.domain.ItemWorkflowStatus;
import com.example.shipping.domain.OrderWorkflowStatus;
import java.util.List;

public record OrderView(
        DemoOrderRecord order,
        String temporalStatus,
        OrderWorkflowStatus workflow,
        List<ItemWorkflowStatus> items) {}
