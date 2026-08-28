package com.example.shipping.domain;

public record OrderRequest(String patientId, String addressId, String preset) {}
