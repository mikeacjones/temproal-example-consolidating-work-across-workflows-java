package com.example.shipping.api;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DemoOrderRegistry {
    private final Map<String, DemoOrderRecord> orders = new ConcurrentHashMap<>();
    private final Map<String, String> itemToOrder = new ConcurrentHashMap<>();

    public void add(DemoOrderRecord order) {
        orders.put(order.orderId(), order);
        order.items().forEach(item -> itemToOrder.put(item.itemWorkflowId(), order.orderId()));
    }

    public List<DemoOrderRecord> all() {
        return orders.values().stream()
                .sorted(Comparator.comparing(DemoOrderRecord::submittedAt).reversed())
                .toList();
    }

    public boolean knowsItem(String workflowId) {
        return itemToOrder.containsKey(workflowId);
    }
}
