package hu.unideb.inf.backend.service.facade;

import hu.unideb.inf.backend.service.facade.dto.StockAlertDto;
import hu.unideb.inf.backend.service.facade.dto.ZoneLoadDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DemoAdminViewFacadeTest {

    private DemoAdminViewFacade facade;

    @BeforeEach
    void setUp() {
        facade = new DemoAdminViewFacade();
    }

    @Test
    @DisplayName("Should return demo statistics")
    void testBasicStats() {
        assertThat(facade.getTotalProducts()).isEqualTo(142);
        assertThat(facade.getOccupancyRate()).isEqualTo("78%");
        assertThat(facade.getCriticalStockCount()).isEqualTo(5);
        assertThat(facade.getActiveTasksCount()).isEqualTo(12);
        assertThat(facade.getCurrentUsername()).isEqualTo("Adminisztrátor");
        assertThat(facade.getCurrentUserRole()).isEqualTo("Diszpécser");
    }

    @Test
    @DisplayName("Should return zone loads and format details correctly")
    void testZoneLoads() {
        List<ZoneLoadDto> loads = facade.getZoneLoads();
        assertThat(loads).hasSize(3);

        ZoneLoadDto normalZone = loads.get(0);
        assertThat(normalZone.getName()).isEqualTo("A-Zóna (Száraz raktár - Normál)");
        assertThat(normalZone.getCurrentWeight()).isEqualTo(4500);
        assertThat(normalZone.getMaxWeight()).isEqualTo(6000);
        assertThat(normalZone.getPercentage()).isEqualTo(75);
        assertThat(normalZone.isCritical()).isFalse();
        assertThat(normalZone.getProgressBarClass()).isEqualTo("bg-primary");
        assertThat(normalZone.getDetailsTextClass()).isEqualTo("text-muted");
        assertThat(normalZone.getDisplayDetails()).isEqualTo("4500 / 6000 kg (75%)");

        ZoneLoadDto criticalZone = loads.get(1);
        assertThat(criticalZone.isCritical()).isTrue();
        assertThat(criticalZone.getDisplayDetails()).contains("- Kritikus!");
    }

    @Test
    @DisplayName("Should return stock alerts with expected fields")
    void testStockAlerts() {
        List<StockAlertDto> alerts = facade.getStockAlerts();
        assertThat(alerts).hasSize(2);

        StockAlertDto alert1 = alerts.get(0);
        assertThat(alert1.getTitle()).isEqualTo("Fékfolyadék DOT4 (SKU-1092)");
        assertThat(alert1.getBadgeText()).isEqualTo("Készlet: 3 db (Min: 10 db)");
        assertThat(alert1.getBadgeSeverity()).isEqualTo("danger");
        assertThat(alert1.getLocation()).isNull();
        assertThat(alert1.getActionLabel()).isEqualTo("Rendelés");

        StockAlertDto alert2 = alerts.get(1);
        assertThat(alert2.getLocation()).isEqualTo("Polc: A-02-1");
        assertThat(alert2.getActionLabel()).isNull();
    }
}
