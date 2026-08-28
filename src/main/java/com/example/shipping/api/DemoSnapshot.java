package com.example.shipping.api;

import com.example.shipping.domain.Patient;
import java.util.List;

public record DemoSnapshot(
        List<Patient> patients,
        TimingView timing,
        List<OrderView> orders,
        List<ConsolidationView> consolidations) {}
