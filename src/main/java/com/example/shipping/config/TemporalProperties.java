package com.example.shipping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("temporal")
public class TemporalProperties {
    private String target = "localhost:7233";
    private String namespace = "default";
    private String taskQueue = "consolidated-shipping-demo";

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getTaskQueue() {
        return taskQueue;
    }

    public void setTaskQueue(String taskQueue) {
        this.taskQueue = taskQueue;
    }
}
