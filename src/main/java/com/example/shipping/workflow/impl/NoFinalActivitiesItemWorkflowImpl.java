package com.example.shipping.workflow.impl;

import com.example.shipping.domain.ItemInput;
import com.example.shipping.domain.ItemResult;
import com.example.shipping.workflow.NoFinalActivitiesItemWorkflow;

public class NoFinalActivitiesItemWorkflowImpl extends AbstractItemWorkflow
        implements NoFinalActivitiesItemWorkflow {

    @Override
    public ItemResult run(ItemInput input) {
        initialize(input);
        step("validate prescription");
        step("prepare item");
        submitForConsolidation();
        waitForShipment();
        return complete();
    }
}
