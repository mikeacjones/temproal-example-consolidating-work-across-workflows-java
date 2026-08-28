package com.example.shipping.api;

import com.example.shipping.domain.Address;
import com.example.shipping.domain.Patient;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DemoCatalog {
    private final List<Patient> patients = List.of(
            new Patient(
                    "patient-a",
                    "Avery Carter",
                    List.of(
                            new Address(
                                    "home",
                                    "Home",
                                    "15 Cedar Lane",
                                    "Raleigh",
                                    "NC",
                                    "27601"),
                            new Address(
                                    "care-center",
                                    "Care center",
                                    "400 Health Plaza",
                                    "Raleigh",
                                    "NC",
                                    "27607"))),
            new Patient(
                    "patient-b",
                    "Morgan Lee",
                    List.of(new Address(
                            "home",
                            "Home",
                            "82 River Street",
                            "Durham",
                            "NC",
                            "27701"))),
            new Patient(
                    "patient-c",
                    "Jordan Rivera",
                    List.of(new Address(
                            "home",
                            "Home",
                            "7 Meadow Court",
                            "Chapel Hill",
                            "NC",
                            "27514"))));

    public List<Patient> patients() {
        return patients;
    }

    public Patient requirePatient(String patientId) {
        return patients.stream()
                .filter(patient -> patient.id().equals(patientId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown patient: " + patientId));
    }

    public Address requireAddress(Patient patient, String addressId) {
        return patient.addresses().stream()
                .filter(address -> address.id().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown address " + addressId + " for patient " + patient.id()));
    }
}
