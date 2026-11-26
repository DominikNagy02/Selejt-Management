package hu.unideb.inf.javafx.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTest {

    @Test
    void newItemStartsWithZeroCountAndEmptyLocations() {
        Item item = new Item(1, "Csavar");

        assertEquals(1, item.getId());
        assertEquals("Csavar", item.getName());
        assertEquals(0, item.getCount());
        assertTrue(item.getLocations().isEmpty());
    }

    @Test
    void addLocationIncreasesCountAndIgnoresBlankValues() {
        Item item = new Item(2, "Kesztyu");

        item.addLocation("S-01-01");
        item.addLocation(null);
        item.addLocation("   ");

        assertEquals(1, item.getCount());
        assertEquals("S-01-01", item.getLocations().get(0));
    }

    @Test
    void settersUpdateIdAndName() {
        Item item = new Item(1, "Regi");

        item.setId(42);
        item.setName("Uj");

        assertEquals(42, item.getId());
        assertEquals("Uj", item.getName());
    }

    @Test
    void modelInitializesDefaultItemWithDecimalId67() {
        Model model = new Model();

        assertNotNull(model.getItem());
        assertEquals(67, model.getItem().getId());
        assertEquals("Fogkefe", model.getItem().getName());
    }
}