package com.tz.statpatterns.math;

public enum DistributionMode {
    BINOMIAL("binomial"),
    NORMAL_APPROXIMATION("normal_approximation");

    private final String serializedName;

    DistributionMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
