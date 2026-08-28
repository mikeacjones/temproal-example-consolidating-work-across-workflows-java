package com.example.shipping.api;

public record OrderSubmission(String orderId, String workflowId, String consolidationWorkflowId) {}
