package com.example.shipping.domain;

import java.util.List;

public record Patient(String id, String name, List<Address> addresses) {}
