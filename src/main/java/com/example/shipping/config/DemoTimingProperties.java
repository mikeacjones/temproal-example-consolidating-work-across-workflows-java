package com.example.shipping.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("demo.timing")
public class DemoTimingProperties {
    private Duration consolidationWindow = Duration.ofHours(8);
    private Duration shipmentActivityDuration = Duration.ofMinutes(1);
    private Duration itemStepDuration = Duration.ofSeconds(3);

    public Duration getConsolidationWindow() {
        return consolidationWindow;
    }

    public void setConsolidationWindow(Duration consolidationWindow) {
        this.consolidationWindow = consolidationWindow;
    }

    public Duration getShipmentActivityDuration() {
        return shipmentActivityDuration;
    }

    public void setShipmentActivityDuration(Duration shipmentActivityDuration) {
        this.shipmentActivityDuration = shipmentActivityDuration;
    }

    public Duration getItemStepDuration() {
        return itemStepDuration;
    }

    public void setItemStepDuration(Duration itemStepDuration) {
        this.itemStepDuration = itemStepDuration;
    }
}
