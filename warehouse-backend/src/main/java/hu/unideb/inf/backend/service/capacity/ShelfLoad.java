package hu.unideb.inf.backend.service.capacity;

public record ShelfLoad(
        String code,
        double currentWeight,
        double currentVolume,
        double maxWeight,
        double maxVolume
) {
}

