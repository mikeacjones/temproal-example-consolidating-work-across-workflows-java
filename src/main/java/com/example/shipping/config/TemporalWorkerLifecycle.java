package com.example.shipping.config;

import com.example.shipping.activity.ConsolidationGatewayActivitiesImpl;
import com.example.shipping.activity.ItemActivitiesImpl;
import com.example.shipping.activity.ShipmentActivitiesImpl;
import com.example.shipping.workflow.impl.ConcurrentActivitiesItemWorkflowImpl;
import com.example.shipping.workflow.impl.ConsolidationWorkflowImpl;
import com.example.shipping.workflow.impl.NoFinalActivitiesItemWorkflowImpl;
import com.example.shipping.workflow.impl.OrderWorkflowImpl;
import com.example.shipping.workflow.impl.PostShipmentActivitiesItemWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class TemporalWorkerLifecycle implements SmartLifecycle {
    private final WorkerFactory factory;
    private volatile boolean running;

    public TemporalWorkerLifecycle(
            WorkflowClient client,
            TemporalProperties temporal,
            DemoTimingProperties timing) {
        factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(temporal.getTaskQueue());
        worker.registerWorkflowImplementationTypes(
                OrderWorkflowImpl.class,
                NoFinalActivitiesItemWorkflowImpl.class,
                PostShipmentActivitiesItemWorkflowImpl.class,
                ConcurrentActivitiesItemWorkflowImpl.class,
                ConsolidationWorkflowImpl.class);
        worker.registerActivitiesImplementations(
                new ItemActivitiesImpl(),
                new ShipmentActivitiesImpl(),
                new ConsolidationGatewayActivitiesImpl(client, temporal, timing));
    }

    @Override
    public void start() {
        factory.start();
        running = true;
    }

    @Override
    public void stop() {
        factory.shutdown();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
