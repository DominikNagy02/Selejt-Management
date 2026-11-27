package hu.unideb.inf.backend.service.suggestion;

import hu.unideb.inf.backend.service.capacity.CapacityCheckResult;
import hu.unideb.inf.backend.service.capacity.CapacityValidator;
import hu.unideb.inf.backend.service.capacity.ItemSize;
import hu.unideb.inf.backend.service.capacity.ShelfLoad;
import hu.unideb.inf.core.enums.ZoneType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class ShelfSuggestionEngine {

    public static final int DEFAULT_MAX_SUGGESTIONS = 5;

    private static final int RATIO_SCALE = 6;
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final Comparator<ShelfSuggestion> BEST_FIT_COMPARATOR =
            Comparator.comparing(ShelfSuggestion::remainingCapacityRatio)
                    .thenComparing(ShelfSuggestion::sameProductPresent, Comparator.reverseOrder())
                    .thenComparing(ShelfSuggestion::shelfCode)
                    .thenComparing(ShelfSuggestion::shelfId);

    private final CapacityValidator capacityValidator;

    public ShelfSuggestionEngine(CapacityValidator capacityValidator) {
        this.capacityValidator = Objects.requireNonNull(capacityValidator, "capacityValidator must not be null");
    }

    /**
     * Suggests up to {@link #DEFAULT_MAX_SUGGESTIONS} active shelves in the required zone that can fit the given quantity,
     * ordered by best-fit utilization.
     */
    public List<ShelfSuggestion> suggest(
            List<ShelfCandidate> candidates,
            ZoneType requiredZone,
            ItemSize item,
            int quantity
    ) {
        return suggest(candidates, requiredZone, item, quantity, DEFAULT_MAX_SUGGESTIONS);
    }

    /**
     * Suggests active shelves in the required zone that have sufficient weight and volume capacity,
     * ranked from tightest fit to most remaining space.
     */
    public List<ShelfSuggestion> suggest(
            List<ShelfCandidate> candidates,
            ZoneType requiredZone,
            ItemSize item,
            int quantity,
            int maxSuggestions
    ) {
        validateInputs(candidates, requiredZone, item, quantity, maxSuggestions);

        return candidates.stream()
                .filter(candidate -> isEligibleZone(candidate, requiredZone))
                .map(candidate -> evaluateCandidate(candidate, item, quantity))
                .flatMap(Optional::stream)
                .sorted(BEST_FIT_COMPARATOR)
                .limit(maxSuggestions)
                .toList();
    }

    private void validateInputs(
            List<ShelfCandidate> candidates,
            ZoneType requiredZone,
            ItemSize item,
            int quantity,
            int maxSuggestions
    ) {
        Objects.requireNonNull(candidates, "candidates must not be null");
        Objects.requireNonNull(requiredZone, "requiredZone must not be null");
        Objects.requireNonNull(item, "item must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        if (maxSuggestions <= 0) {
            throw new IllegalArgumentException("maxSuggestions must be greater than 0");
        }
        for (ShelfCandidate candidate : candidates) {
            Objects.requireNonNull(candidate, "candidate must not be null");
        }
    }

    private boolean isEligibleZone(ShelfCandidate candidate, ZoneType requiredZone) {
        return candidate.active() && candidate.zoneType() == requiredZone;
    }

    private Optional<ShelfSuggestion> evaluateCandidate(ShelfCandidate candidate, ItemSize item, int quantity) {
        CapacityCheckResult checkResult = capacityValidator.check(candidate.load(), item, quantity);
        if (!checkResult.isAllowed()) {
            return Optional.empty();
        }
        return Optional.of(toSuggestion(candidate, checkResult));
    }

    private ShelfSuggestion toSuggestion(ShelfCandidate candidate, CapacityCheckResult checkResult) {
        ShelfLoad load = candidate.load();
        BigDecimal freeWeight = load.maxWeight().subtract(load.currentWeight());
        BigDecimal freeVolume = load.maxVolume().subtract(load.currentVolume());
        BigDecimal remainingWeight = load.maxWeight().subtract(checkResult.newWeight());
        BigDecimal remainingVolume = load.maxVolume().subtract(checkResult.newVolume());
        BigDecimal remainingRatio = calculateRemainingCapacityRatio(load, remainingWeight, remainingVolume);

        return new ShelfSuggestion(
                candidate.shelfId(),
                load.code(),
                candidate.zoneType(),
                freeWeight,
                freeVolume,
                remainingWeight,
                remainingVolume,
                remainingRatio,
                candidate.containsSameProduct()
        );
    }

    private BigDecimal calculateRemainingCapacityRatio(
            ShelfLoad load,
            BigDecimal remainingWeight,
            BigDecimal remainingVolume
    ) {
        BigDecimal weightRatio = remainingWeight.divide(load.maxWeight(), RATIO_SCALE, RoundingMode.HALF_UP);
        BigDecimal volumeRatio = remainingVolume.divide(load.maxVolume(), RATIO_SCALE, RoundingMode.HALF_UP);
        return weightRatio.add(volumeRatio).divide(TWO, RATIO_SCALE, RoundingMode.HALF_UP);
    }
}

