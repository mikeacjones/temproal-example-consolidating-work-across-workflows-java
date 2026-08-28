package com.example.shipping.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shipping.activity.ShipmentActivities;
import com.example.shipping.domain.ConsolidationRunInput;
import com.example.shipping.domain.ConsolidationWorkflowStatus;
import com.example.shipping.domain.ShipmentBatch;
import com.example.shipping.domain.ShipmentItem;
import com.example.shipping.domain.ShipmentResult;
import com.example.shipping.workflow.impl.ConsolidationWorkflowImpl;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsolidationWorkflowTest {
    private static final String TASK_QUEUE = "consolidation-test";

    private TestWorkflowEnvironment environment;
    private WorkflowClient client;
    private BlockingShipmentActivities shipments;

    @BeforeEach
    void setUp() {
        environment = TestWorkflowEnvironment.newInstance();
        client = environment.getWorkflowClient();
        shipments = new BlockingShipmentActivities();

        Worker worker = environment.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(
                ConsolidationWorkflowImpl.class,
                TestItemReceiverWorkflowImpl.class);
        worker.registerActivitiesImplementations(shipments);
        environment.start();
    }

    @AfterEach
    void tearDown() {
        environment.close();
    }

    @Test
    void itemsArrivingDuringShipmentContinueAsNewIntoTheirOwnTimedBatch() throws Exception {
        startReceiver("test-item-one");
        startReceiver("test-item-two");

        WorkflowStub consolidator = client.newUntypedWorkflowStub(
                "ConsolidationWorkflow",
                WorkflowOptions.newBuilder()
                        .setWorkflowId("consolidation-patient-a-home")
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowIdReusePolicy(
                                WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
                        .build());
        ConsolidationRunInput input = ConsolidationRunInput.firstRun(
                "patient-a", "home", 0, 0);

        consolidator.signalWithStart(
                "addItem",
                new Object[] {item("submission-one", "item-one", "test-item-one")},
                new Object[] {input});

        assertTrue(shipments.firstStarted.await(5, TimeUnit.SECONDS));
        consolidator.signal("addItem", item("submission-two", "item-two", "test-item-two"));
        shipments.releaseFirst.countDown();

        assertTrue(shipments.secondStarted.await(5, TimeUnit.SECONDS));
        ConsolidationWorkflow currentRun = client.newWorkflowStub(
                ConsolidationWorkflow.class, "consolidation-patient-a-home");
        ConsolidationWorkflowStatus status = currentRun.status();
        assertEquals(2, status.batchNumber());
        assertEquals("SHIPPING", status.phase());
        assertEquals(List.of("item-two"),
                status.currentBatchItems().stream().map(ShipmentItem::itemId).toList());

        shipments.releaseSecond.countDown();
        ShipmentResult firstResult = receiverResult("test-item-one");
        ShipmentResult secondResult = receiverResult("test-item-two");

        assertEquals(1, firstResult.batchNumber());
        assertEquals(2, secondResult.batchNumber());
        assertEquals(
                List.of(List.of("item-one"), List.of("item-two")),
                shipments.batches.stream()
                        .map(batch -> batch.items().stream().map(ShipmentItem::itemId).toList())
                        .toList());
    }

    private ShipmentItem item(String submissionId, String itemId, String workflowId) {
        return new ShipmentItem(
                submissionId,
                "patient-a",
                "home",
                "order-one",
                itemId,
                workflowId,
                "SKU-1",
                "Test item");
    }

    private void startReceiver(String workflowId) {
        TestItemReceiverWorkflow receiver = client.newWorkflowStub(
                TestItemReceiverWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(TASK_QUEUE)
                        .build());
        WorkflowClient.start(receiver::run);
    }

    private ShipmentResult receiverResult(String workflowId) {
        return client.newWorkflowStub(TestItemReceiverWorkflow.class, workflowId).run();
    }

    private static final class BlockingShipmentActivities implements ShipmentActivities {
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch firstStarted = new CountDownLatch(1);
        private final CountDownLatch releaseFirst = new CountDownLatch(1);
        private final CountDownLatch secondStarted = new CountDownLatch(1);
        private final CountDownLatch releaseSecond = new CountDownLatch(1);
        private final List<ShipmentBatch> batches = new CopyOnWriteArrayList<>();

        @Override
        public ShipmentResult consolidateAndShip(ShipmentBatch batch) {
            batches.add(batch);
            int call = calls.incrementAndGet();
            CountDownLatch started = call == 1 ? firstStarted : secondStarted;
            CountDownLatch release = call == 1 ? releaseFirst : releaseSecond;
            started.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Test did not release shipment activity " + call);
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
            return new ShipmentResult(
                    batch.batchId(), batch.batchNumber(), true, "TEST-" + call, "shipped");
        }
    }

    @WorkflowInterface
    public interface TestItemReceiverWorkflow {
        @WorkflowMethod(name = "TestItemReceiverWorkflow")
        ShipmentResult run();

        @SignalMethod(name = "shipmentCompleted")
        void shipmentCompleted(ShipmentResult result);
    }

    public static class TestItemReceiverWorkflowImpl implements TestItemReceiverWorkflow {
        private ShipmentResult result;

        @Override
        public ShipmentResult run() {
            Workflow.await(() -> result != null);
            return result;
        }

        @Override
        public void shipmentCompleted(ShipmentResult result) {
            this.result = result;
        }
    }
}
