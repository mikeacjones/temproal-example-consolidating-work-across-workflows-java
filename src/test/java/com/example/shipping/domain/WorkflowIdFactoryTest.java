package com.example.shipping.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorkflowIdFactoryTest {
    @Test
    void patientAndAddressProduceAStableConsolidationId() {
        assertEquals(
                "consolidation-patient-a-care-center",
                WorkflowIdFactory.consolidation("Patient A", "Care Center"));
    }
}
