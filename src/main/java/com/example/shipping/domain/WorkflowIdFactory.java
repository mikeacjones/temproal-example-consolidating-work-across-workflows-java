package com.example.shipping.domain;

import java.util.Locale;

public final class WorkflowIdFactory {
    private WorkflowIdFactory() {}

    public static String order(String orderId) {
        return "order-" + safe(orderId);
    }

    public static String item(String orderId, String itemId) {
        return "item-" + safe(orderId) + "-" + safe(itemId);
    }

    public static String consolidation(String patientId, String addressId) {
        return "consolidation-" + safe(patientId) + "-" + safe(addressId);
    }

    private static String safe(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
    }
}
