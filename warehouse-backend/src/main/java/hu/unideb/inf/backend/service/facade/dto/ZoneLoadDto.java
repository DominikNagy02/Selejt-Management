package hu.unideb.inf.backend.service.facade.dto;

import java.util.Locale;

public record ZoneLoadDto(
        String name,
        double currentWeight,
        double maxWeight,
        int percentage,
        boolean critical,
        String progressBarClass,
        String detailsTextClass
) {
    public String getDisplayDetails() {
        return String.format(Locale.US, "%.0f / %.0f kg (%d%%%s)",
                currentWeight, maxWeight, percentage, critical ? " - Kritikus!" : "");
    }

    public String getName() {
        return name;
    }

    public double getCurrentWeight() {
        return currentWeight;
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public int getPercentage() {
        return percentage;
    }

    public boolean isCritical() {
        return critical;
    }

    public String getProgressBarClass() {
        return progressBarClass;
    }

    public String getDetailsTextClass() {
        return detailsTextClass;
    }
}
