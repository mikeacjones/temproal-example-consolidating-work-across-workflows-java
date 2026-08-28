package com.example.shipping.activity;

import com.example.shipping.domain.ActivityStep;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ItemActivities {
    @ActivityMethod
    String performStep(ActivityStep step);
}
