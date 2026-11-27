package hu.unideb.inf.backend.service.suggestion;

import hu.unideb.inf.backend.service.capacity.ShelfLoad;
import hu.unideb.inf.core.enums.ZoneType;

import java.util.Objects;

public record ShelfCandidate(
        Long shelfId,
        ShelfLoad load,
        ZoneType zoneType,
        boolean active,
        boolean containsSameProduct
) {
    public ShelfCandidate {
        Objects.requireNonNull(shelfId, "shelfId must not be null");
        Objects.requireNonNull(load, "load must not be null");
        Objects.requireNonNull(zoneType, "zoneType must not be null");
    }

    public ShelfCandidate(Long shelfId, ShelfLoad load, ZoneType zoneType, boolean active) {
        this(shelfId, load, zoneType, active, false);
    }
}

