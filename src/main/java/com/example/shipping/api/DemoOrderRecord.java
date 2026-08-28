package com.example.shipping.api;

import com.example.shipping.domain.ItemInput;
import java.time.Instant;
import java.util.List;

public record DemoOrderRecord(
        String orderId,
        String orderWorkflowId,
        String patientId,
        String addressId,
        String consolidationWorkflowId,
        String preset,
        Instant submittedAt,
        List<ItemInput> items) {}
