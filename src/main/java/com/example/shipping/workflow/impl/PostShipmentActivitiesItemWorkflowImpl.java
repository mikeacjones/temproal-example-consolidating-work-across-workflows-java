package com.example.shipping.workflow.impl;

import com.example.shipping.domain.ItemInput;
import com.example.shipping.domain.ItemResult;
import com.example.shipping.workflow.PostShipmentActivitiesItemWorkflow;

public class PostShipmentActivitiesItemWorkflowImpl extends AbstractItemWorkflow
        implements PostShipmentActivitiesItemWorkflow {

    @Override
    public ItemResult run(ItemInput input) {
        initialize(input);
        step("clinical review");
        waitForApproval();
        step("prepare approved item");
        submitForConsolidation();
        waitForShipment();
        step("send follow-up instructions");
        step("archive approval record");
        return complete();
    }
}
