package com.example.shipping.domain;

import java.util.List;

public record ItemWorkflowStatus(
        String itemId,
        String itemWorkflowId,
        String description,
        ItemBehavior behavior,
        String state,
        boolean awaitingApproval,
        boolean approved,
        List<String> completedSteps,
        RegistrationReceipt registration,
        ShipmentResult shipment) {}
