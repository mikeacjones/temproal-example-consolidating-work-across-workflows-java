package com.example.shipping.workflow.impl;

import com.example.shipping.domain.ItemInput;
import com.example.shipping.domain.ItemResult;
import com.example.shipping.workflow.ConcurrentActivitiesItemWorkflow;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;

public class ConcurrentActivitiesItemWorkflowImpl extends AbstractItemWorkflow
        implements ConcurrentActivitiesItemWorkflow {

    @Override
    public ItemResult run(ItemInput input) {
        initialize(input);
        step("validate specialty item");
        step("prepare specialty item");
        submitForConsolidation();

        state = "RUNNING_ACTIVITIES_WHILE_SHIPMENT_IS_PENDING";
        Promise<Void> notifyCareTeam = Async.procedure(() -> step("notify care team"));
        Promise<Void> updatePatientRecord = Async.procedure(() -> step("update patient record"));
        Promise.allOf(notifyCareTeam, updatePatientRecord).get();

        waitForShipment();
        return complete();
    }
}
