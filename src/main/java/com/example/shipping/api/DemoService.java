package com.example.shipping.api;

import com.example.shipping.config.DemoTimingProperties;
import com.example.shipping.config.TemporalProperties;
import com.example.shipping.domain.ConsolidationWorkflowStatus;
import com.example.shipping.domain.ItemBehavior;
import com.example.shipping.domain.ItemInput;
import com.example.shipping.domain.ItemWorkflowStatus;
import com.example.shipping.domain.OrderInput;
import com.example.shipping.domain.OrderRequest;
import com.example.shipping.domain.OrderWorkflowStatus;
import com.example.shipping.domain.Patient;
import com.example.shipping.domain.WorkflowIdFactory;
import com.example.shipping.workflow.ConsolidationWorkflow;
import com.example.shipping.workflow.OrderWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DemoService {
    private final WorkflowClient client;
    private final TemporalProperties temporal;
    private final DemoTimingProperties timing;
    private final DemoCatalog catalog;
    private final DemoOrderRegistry registry;

    public DemoService(
            WorkflowClient client,
            TemporalProperties temporal,
            DemoTimingProperties timing,
            DemoCatalog catalog,
            DemoOrderRegistry registry) {
        this.client = client;
        this.temporal = temporal;
        this.timing = timing;
        this.catalog = catalog;
        this.registry = registry;
    }

    public OrderSubmission submitOrder(OrderRequest request) {
        Patient patient = catalog.requirePatient(request.patientId());
        catalog.requireAddress(patient, request.addressId());

        String orderId = UUID.randomUUID().toString().substring(0, 8);
        String orderWorkflowId = WorkflowIdFactory.order(orderId);
        String consolidationWorkflowId =
                WorkflowIdFactory.consolidation(request.patientId(), request.addressId());
        List<ItemInput> items = createItems(
                request.preset() == null ? "showcase" : request.preset(),
                orderId,
                request.patientId(),
                request.addressId(),
                consolidationWorkflowId);

        OrderInput input = new OrderInput(
                orderId,
                request.patientId(),
                request.addressId(),
                consolidationWorkflowId,
                timing.getItemStepDuration().toMillis(),
                items);
        OrderWorkflow workflow = client.newWorkflowStub(
                OrderWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(orderWorkflowId)
                        .setTaskQueue(temporal.getTaskQueue())
                        .build());
        WorkflowClient.start(workflow::run, input);

        registry.add(new DemoOrderRecord(
                orderId,
                orderWorkflowId,
                request.patientId(),
                request.addressId(),
                consolidationWorkflowId,
                request.preset(),
                Instant.now(),
                items));
        return new OrderSubmission(orderId, orderWorkflowId, consolidationWorkflowId);
    }

    public void approve(String itemWorkflowId) {
        if (!registry.knowsItem(itemWorkflowId)) {
            throw new IllegalArgumentException("Unknown demo item workflow: " + itemWorkflowId);
        }
        client.newUntypedWorkflowStub(itemWorkflowId).signal("approve");
    }

    public DemoSnapshot snapshot() {
        List<OrderView> orders = registry.all().stream().map(this::orderView).toList();

        Map<String, DemoOrderRecord> uniqueConsolidations = new LinkedHashMap<>();
        registry.all().forEach(order -> uniqueConsolidations.put(order.consolidationWorkflowId(), order));
        List<ConsolidationView> consolidations = uniqueConsolidations.values().stream()
                .map(this::consolidationView)
                .toList();

        return new DemoSnapshot(catalog.patients(), timingView(), orders, consolidations);
    }

    private OrderView orderView(DemoOrderRecord order) {
        OrderWorkflowStatus workflowStatus = null;
        String temporalStatus = "RUNNING";
        try {
            OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class, order.orderWorkflowId());
            workflowStatus = workflow.status();
            temporalStatus = workflowStatus.state();
        } catch (Exception error) {
            temporalStatus = "QUERY_UNAVAILABLE";
        }

        List<ItemWorkflowStatus> itemStatuses = new ArrayList<>();
        for (ItemInput item : order.items()) {
            try {
                WorkflowStub workflow = client.newUntypedWorkflowStub(item.itemWorkflowId());
                itemStatuses.add(workflow.query("status", ItemWorkflowStatus.class));
            } catch (Exception error) {
                itemStatuses.add(new ItemWorkflowStatus(
                        item.itemId(),
                        item.itemWorkflowId(),
                        item.description(),
                        item.behavior(),
                        "STARTING_OR_QUERY_UNAVAILABLE",
                        false,
                        false,
                        List.of(),
                        null,
                        null));
            }
        }
        return new OrderView(order, temporalStatus, workflowStatus, itemStatuses);
    }

    private ConsolidationView consolidationView(DemoOrderRecord order) {
        try {
            ConsolidationWorkflow workflow = client.newWorkflowStub(
                    ConsolidationWorkflow.class, order.consolidationWorkflowId());
            ConsolidationWorkflowStatus status = workflow.status();
            return new ConsolidationView(
                    order.consolidationWorkflowId(),
                    order.patientId(),
                    order.addressId(),
                    status.phase(),
                    status);
        } catch (Exception error) {
            return new ConsolidationView(
                    order.consolidationWorkflowId(),
                    order.patientId(),
                    order.addressId(),
                    "NOT_STARTED_OR_QUERY_UNAVAILABLE",
                    null);
        }
    }

    private TimingView timingView() {
        return new TimingView(
                timing.getConsolidationWindow().toString(),
                timing.getConsolidationWindow().toMillis(),
                timing.getShipmentActivityDuration().toString(),
                timing.getShipmentActivityDuration().toMillis(),
                timing.getItemStepDuration().toString(),
                timing.getItemStepDuration().toMillis());
    }

    private List<ItemInput> createItems(
            String preset,
            String orderId,
            String patientId,
            String addressId,
            String consolidationWorkflowId) {
        List<ItemTemplate> templates = switch (preset) {
            case "single" -> List.of(new ItemTemplate(
                    "RX-101", "Routine refill", ItemBehavior.NO_FINAL_ACTIVITIES));
            case "approval" -> List.of(new ItemTemplate(
                    "RX-202", "Approval and post-shipment follow-up",
                    ItemBehavior.FINAL_ACTIVITIES_AFTER_SHIPMENT));
            case "showcase" -> List.of(
                    new ItemTemplate(
                            "RX-101", "No activities after shipment",
                            ItemBehavior.NO_FINAL_ACTIVITIES),
                    new ItemTemplate(
                            "RX-202", "Human approval then post-shipment activities",
                            ItemBehavior.FINAL_ACTIVITIES_AFTER_SHIPMENT),
                    new ItemTemplate(
                            "RX-303", "Activities run concurrently while shipment is pending",
                            ItemBehavior.CONCURRENT_ACTIVITIES_WHILE_SHIPPING));
            default -> throw new IllegalArgumentException("Unknown order preset: " + preset);
        };

        List<ItemInput> items = new ArrayList<>();
        for (int index = 0; index < templates.size(); index++) {
            ItemTemplate template = templates.get(index);
            String itemId = "item-" + (index + 1);
            items.add(new ItemInput(
                    orderId,
                    itemId,
                    WorkflowIdFactory.item(orderId, itemId),
                    patientId,
                    addressId,
                    consolidationWorkflowId,
                    template.sku(),
                    template.description(),
                    template.behavior(),
                    timing.getItemStepDuration().toMillis()));
        }
        return items;
    }

    private record ItemTemplate(String sku, String description, ItemBehavior behavior) {}
}
