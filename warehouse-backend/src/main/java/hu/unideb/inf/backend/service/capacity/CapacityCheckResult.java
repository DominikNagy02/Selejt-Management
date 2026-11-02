package hu.unideb.inf.backend.service.capacity;

public record CapacityCheckResult(
        double newWeight,
        double newVolume,
        boolean weightOk,
        boolean volumeOk
) {
    public boolean isAllowed() {
        return weightOk && volumeOk;
    }
}

