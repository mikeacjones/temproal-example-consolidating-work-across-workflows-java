package com.example.shipping.activity;

import com.example.shipping.domain.ActivityStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemActivitiesImpl implements ItemActivities {
    private static final Logger log = LoggerFactory.getLogger(ItemActivitiesImpl.class);

    @Override
    public String performStep(ActivityStep step) {
        log.info("Item {} executing step {}", step.itemWorkflowId(), step.stepName());
        SyntheticWait.withHeartbeats(step.syntheticDurationMillis(), step.stepName());
        return step.stepName();
    }
}
