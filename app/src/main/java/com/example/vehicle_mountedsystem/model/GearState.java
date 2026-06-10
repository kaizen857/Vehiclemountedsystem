package com.example.vehicle_mountedsystem.model;

public enum GearState {
    P,
    R,
    N,
    D,
    S;

    public static GearState defaultState() {
        return P;
    }

    public static GearState fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("gear must not be null");
        }
        for (GearState gear : values()) {
            if (gear.name().equals(value.trim())) {
                return gear;
            }
        }
        throw new IllegalArgumentException("gear must be one of P/R/N/D/S");
    }
}
