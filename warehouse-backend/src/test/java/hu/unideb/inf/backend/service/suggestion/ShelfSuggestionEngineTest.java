package hu.unideb.inf.backend.service.suggestion;

import hu.unideb.inf.backend.service.capacity.CapacityCalculator;
import hu.unideb.inf.backend.service.capacity.CapacityValidator;
import hu.unideb.inf.backend.service.capacity.ItemSize;
import hu.unideb.inf.backend.service.capacity.ShelfLoad;
import hu.unideb.inf.core.enums.ZoneType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShelfSuggestionEngineTest {

    private ShelfSuggestionEngine engine;
    private ItemSize defaultItem;

    @BeforeEach
    void setUp() {
        engine = new ShelfSuggestionEngine(new CapacityValidator(new CapacityCalculator()));
        defaultItem = new ItemSize(new BigDecimal("2.000"), new BigDecimal("0.020"));
    }

    // F6.10: Szűrés zónatípusra és aktív állapotra
    @Test
    void suggestFiltersOutMismatchedZonesAndInactiveShelves() {
        ShelfCandidate normalShelf = new ShelfCandidate(
                1L,
                new ShelfLoad("N-01-01", new BigDecimal("100"), new BigDecimal("1.0"), new BigDecimal("500"), new BigDecimal("5.0")),
                ZoneType.NORMAL,
                true
        );
        ShelfCandidate hazardousShelf = candidate(2L, "H-01-01", "100", "1.0", "500", "5.0", ZoneType.HAZARDOUS, true, false);
        ShelfCandidate inactiveChilledShelf = candidate(3L, "C-01-01", "100", "1.0", "500", "5.0", ZoneType.CHILLED, false, false);
        ShelfCandidate activeChilledShelf = candidate(4L, "C-01-02", "100", "1.0", "500", "5.0", ZoneType.CHILLED, true, false);

        List<ShelfSuggestion> suggestions = engine.suggest(
                List.of(normalShelf, hazardousShelf, inactiveChilledShelf, activeChilledShelf),
                ZoneType.CHILLED,
                defaultItem,
                10
        );

        assertEquals(1, suggestions.size());
        ShelfSuggestion suggestion = suggestions.get(0);
        assertEquals(4L, suggestion.shelfId());
        assertEquals("C-01-02", suggestion.shelfCode());
        assertEquals(ZoneType.CHILLED, suggestion.zoneType());
    }

    // F6.11: Szűrés szabad kapacitásra (súly/térfogat túllépés kizárása, pontos illeszkedés megtartása)
    @Test
    void suggestFiltersOutShelvesWithoutEnoughCapacityAndAllowsExactFit() {
        // Új tétel: 20 db * (2 kg, 0.02 m3) = +40 kg, +0.4 m3
        ShelfCandidate weightExceeded = candidate(1L, "N-01-01", "470.000", "1.000", "500.000", "5.000", ZoneType.NORMAL, true, false);
        ShelfCandidate volumeExceeded = candidate(2L, "N-01-02", "100.000", "4.700", "500.000", "5.000", ZoneType.NORMAL, true, false);
        ShelfCandidate exactFit = candidate(3L, "N-01-03", "460.000", "4.600", "500.000", "5.000", ZoneType.NORMAL, true, true);

        List<ShelfSuggestion> suggestions = engine.suggest(
                List.of(weightExceeded, volumeExceeded, exactFit),
                ZoneType.NORMAL,
                defaultItem,
                20
        );

        assertEquals(1, suggestions.size());
        ShelfSuggestion suggestion = suggestions.get(0);
        assertEquals("N-01-03", suggestion.shelfCode());
        assertEquals(0, new BigDecimal("40.000").compareTo(suggestion.freeWeightKg()));
        assertEquals(0, new BigDecimal("0.400").compareTo(suggestion.freeVolumeM3()));
        assertEquals(0, BigDecimal.ZERO.compareTo(suggestion.remainingWeightAfterPlacementKg()));
        assertEquals(0, BigDecimal.ZERO.compareTo(suggestion.remainingVolumeAfterPlacementM3()));
        assertEquals(0, BigDecimal.ZERO.compareTo(suggestion.remainingCapacityRatio()));
        assertTrue(suggestion.sameProductPresent());
    }

    // F6.12: Best-fit rangsorolás + másodlagos azonos-termék és determinisztikus kód/ID sorrend
    @Test
    void suggestRanksCandidatesByBestFitWithSameProductAndCodeTieBreakers() {
        // Bevételezés: 10 db * (2 kg, 0.02 m3) = +20 kg, +0.2 m3
        // Mindegyik polc 500 kg / 5.0 m3 max:
        // - tighterNoSame: elhelyezés után marad 50 kg (10%), 0.5 m3 (10%) -> átlag maradék: 0.10
        // - looserSameProduct: elhelyezés után marad 300 kg (60%), 3.0 m3 (60%) -> átlag maradék: 0.60
        // - mediumSameProduct & mediumDifferentCode & mediumDuplicateCode: maradék 150 kg (30%), 1.5 m3 (30%) -> átlag: 0.30
        ShelfCandidate looserSameProduct = candidate(10L, "A-01-01", "180", "1.8", "500", "5.0", ZoneType.NORMAL, true, true);
        ShelfCandidate mediumNoSameA = candidate(11L, "B-01-01", "330", "3.3", "500", "5.0", ZoneType.NORMAL, true, false);
        ShelfCandidate mediumSameZ = candidate(12L, "Z-01-01", "330", "3.3", "500", "5.0", ZoneType.NORMAL, true, true);
        ShelfCandidate mediumNoSameCId5 = candidate(5L, "C-01-01", "330", "3.3", "500", "5.0", ZoneType.NORMAL, true, false);
        ShelfCandidate mediumNoSameCId2 = candidate(2L, "C-01-01", "330", "3.3", "500", "5.0", ZoneType.NORMAL, true, false);
        ShelfCandidate tighterNoSame = candidate(13L, "D-01-01", "430", "4.3", "500", "5.0", ZoneType.NORMAL, true, false);

        List<ShelfSuggestion> suggestions = engine.suggest(
                List.of(looserSameProduct, mediumNoSameA, mediumSameZ, mediumNoSameCId5, mediumNoSameCId2, tighterNoSame),
                ZoneType.NORMAL,
                defaultItem,
                10,
                6
        );

        assertEquals(6, suggestions.size());
        assertEquals(List.of(13L, 12L, 11L, 2L, 5L, 10L),
                suggestions.stream().map(ShelfSuggestion::shelfId).toList());
        assertFalse(suggestions.get(0).sameProductPresent());
        assertTrue(suggestions.get(1).sameProductPresent());
    }

    // F6.12: Két eltérő méretű üres polcnál a kisebb (jobban kihasznált) kerül előre
    @Test
    void suggestPrefersSmallerMatchingShelfOverOversizedShelf() {
        ItemSize heavyItem = new ItemSize(new BigDecimal("8.000"), new BigDecimal("0.080"));
        ShelfCandidate smallShelf = candidate(1L, "S-SMALL", "0", "0", "100", "1.0", ZoneType.NORMAL, true, false);
        ShelfCandidate largeShelf = candidate(2L, "S-LARGE", "0", "0", "500", "5.0", ZoneType.NORMAL, true, false);

        List<ShelfSuggestion> suggestions = engine.suggest(
                List.of(largeShelf, smallShelf),
                ZoneType.NORMAL,
                heavyItem,
                10
        );

        assertEquals(2, suggestions.size());
        assertEquals("S-SMALL", suggestions.get(0).shelfCode());
        assertEquals("S-LARGE", suggestions.get(1).shelfCode());
    }

    // F6.12: Alapértelmezett N = 5 és egyedi limit ellenőrzése
    @Test
    void suggestLimitsResultsToDefaultFiveAndCustomLimit() {
        List<ShelfCandidate> sevenCandidates = List.of(
                candidate(1L, "S-01", "460", "4.6", "500", "5.0", ZoneType.NORMAL, true, false),
                candidate(2L, "S-02", "440", "4.4", "500", "5.0", ZoneType.NORMAL, true, false),
                candidate(3L, "S-03", "420", "4.2", "500", "5.0", ZoneType.NORMAL, true, false),
                candidate(4L, "S-04", "400", "4.0", "500", "5.0", ZoneType.NORMAL, true, false),
                candidate(5L, "S-05", "380", "3.8", "500", "5.0", ZoneType.NORMAL, true, false),
                candidate(6L, "S-06", "360", "3.6", "500", "5.0", ZoneType.NORMAL, true, false),
                candidate(7L, "S-07", "340", "3.4", "500", "5.0", ZoneType.NORMAL, true, false)
        );

        List<ShelfSuggestion> defaultLimited = engine.suggest(sevenCandidates, ZoneType.NORMAL, defaultItem, 10);
        assertEquals(5, defaultLimited.size());
        assertEquals(List.of("S-01", "S-02", "S-03", "S-04", "S-05"),
                defaultLimited.stream().map(ShelfSuggestion::shelfCode).toList());

        List<ShelfSuggestion> topTwo = engine.suggest(sevenCandidates, ZoneType.NORMAL, defaultItem, 10, 2);
        assertEquals(List.of("S-01", "S-02"),
                topTwo.stream().map(ShelfSuggestion::shelfCode).toList());
    }

    // F6.16: Nincs megfelelő polc -> üres lista (nem kivétel)
    @Test
    void suggestReturnsEmptyListWhenNoShelfMatches() {
        assertTrue(engine.suggest(List.of(), ZoneType.NORMAL, defaultItem, 10).isEmpty());

        ShelfCandidate wrongZone = candidate(1L, "N-01", "0", "0", "500", "5.0", ZoneType.NORMAL, true, false);
        ShelfCandidate tooFull = candidate(2L, "C-01", "495", "4.9", "500", "5.0", ZoneType.CHILLED, true, false);
        assertTrue(engine.suggest(List.of(wrongZone, tooFull), ZoneType.CHILLED, defaultItem, 10).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5})
    void suggestRejectsNonPositiveQuantityAndMaxSuggestions(int invalidValue) {
        List<ShelfCandidate> candidates = List.of();
        assertThrows(IllegalArgumentException.class,
                () -> engine.suggest(candidates, ZoneType.NORMAL, defaultItem, invalidValue));
        assertThrows(IllegalArgumentException.class,
                () -> engine.suggest(candidates, ZoneType.NORMAL, defaultItem, 1, invalidValue));
    }

    @Test
    void rejectsNullInputsAndInvalidRecordFields() {
        assertThrows(NullPointerException.class, () -> new ShelfSuggestionEngine(null));

        ShelfCandidate valid = candidate(1L, "S-01", "0", "0", "100", "1.0", ZoneType.NORMAL, true, false);
        List<ShelfCandidate> validList = List.of(valid);
        List<ShelfCandidate> listWithNull = Arrays.asList(valid, null);

        assertThrows(NullPointerException.class, () -> engine.suggest(null, ZoneType.NORMAL, defaultItem, 1));
        assertThrows(NullPointerException.class, () -> engine.suggest(validList, null, defaultItem, 1));
        assertThrows(NullPointerException.class, () -> engine.suggest(validList, ZoneType.NORMAL, null, 1));
        assertThrows(NullPointerException.class, () -> engine.suggest(listWithNull, ZoneType.NORMAL, defaultItem, 1));

        ShelfLoad load = new ShelfLoad("S-01", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE);
        assertThrows(NullPointerException.class, () -> new ShelfCandidate(null, load, ZoneType.NORMAL, true));
        assertThrows(NullPointerException.class, () -> new ShelfCandidate(1L, null, ZoneType.NORMAL, true));
        assertThrows(NullPointerException.class, () -> new ShelfCandidate(1L, load, null, true));

        assertThrows(IllegalArgumentException.class, () -> new ShelfSuggestion(
                1L, "  ", ZoneType.NORMAL,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false
        ));
    }

    private ShelfCandidate candidate(
            Long shelfId,
            String code,
            String currentWeight,
            String currentVolume,
            String maxWeight,
            String maxVolume,
            ZoneType zoneType,
            boolean active,
            boolean containsSameProduct
    ) {
        ShelfLoad load = new ShelfLoad(
                code,
                new BigDecimal(currentWeight),
                new BigDecimal(currentVolume),
                new BigDecimal(maxWeight),
                new BigDecimal(maxVolume)
        );
        return new ShelfCandidate(shelfId, load, zoneType, active, containsSameProduct);
    }
}
