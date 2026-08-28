package com.example.shipping.activity;

import io.temporal.activity.Activity;

final class SyntheticWait {
    private SyntheticWait() {}

    static void withHeartbeats(long durationMillis, String detail) {
        long remaining = Math.max(0, durationMillis);
        while (remaining > 0) {
            long slice = Math.min(1_000, remaining);
            try {
                Thread.sleep(slice);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Synthetic activity was interrupted", interrupted);
            }
            remaining -= slice;
            Activity.getExecutionContext().heartbeat(detail + "; remainingMs=" + remaining);
        }
    }
}
