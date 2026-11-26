package hu.unideb.inf.backend.service.facade;

import hu.unideb.inf.backend.service.facade.dto.StockAlertDto;
import hu.unideb.inf.backend.service.facade.dto.ZoneLoadDto;
import hu.unideb.inf.core.dto.ProductRequest;
import hu.unideb.inf.core.dto.ProductResponse;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoAdminViewFacadeTest {

    private DemoAdminViewFacade facade;

    @BeforeEach
    void setUp() {
        facade = new DemoAdminViewFacade();
    }

    @Test
    @DisplayName("Should return demo statistics")
    void testBasicStats() {
        assertThat(facade.getTotalProducts()).isEqualTo(4);
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

    @Test
    @DisplayName("Should return all products when no filters applied")
    void testGetProducts_noFilters() {
        List<ProductResponse> products = facade.getProducts(null, null);
        assertThat(products).hasSize(4);
    }

    @Test
    @DisplayName("Should filter products by query across sku, name and barcode")
    void testGetProducts_filterQuery() {
        List<ProductResponse> bySku = facade.getProducts("1092", null);
        assertThat(bySku).hasSize(1);
        assertThat(bySku.get(0).sku()).isEqualTo("SKU-1092");

        List<ProductResponse> byName = facade.getProducts("motorolaj", null);
        assertThat(byName).hasSize(1);
        assertThat(byName.get(0).sku()).isEqualTo("SKU-2044");

        List<ProductResponse> byBarcode = facade.getProducts("5901234567892", null);
        assertThat(byBarcode).hasSize(1);
        assertThat(byBarcode.get(0).sku()).isEqualTo("SKU-3011");
    }

    @Test
    @DisplayName("Should filter products by zone type")
    void testGetProducts_filterZoneType() {
        List<ProductResponse> hazardous = facade.getProducts(null, ZoneType.HAZARDOUS);
        assertThat(hazardous).hasSize(1);
        assertThat(hazardous.get(0).requiredZoneType()).isEqualTo(ZoneType.HAZARDOUS);

        List<ProductResponse> normal = facade.getProducts(null, ZoneType.NORMAL);
        assertThat(normal).hasSize(2);
    }

    @Test
    @DisplayName("Should find product by id or throw ResourceNotFoundException")
    void testGetProductById() {
        ProductResponse product = facade.getProductById(1L);
        assertThat(product.name()).isEqualTo("Fékfolyadék DOT4 500ml");

        assertThatThrownBy(() -> facade.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should create product and assign generated id")
    void testCreateProduct() {
        ProductRequest req = new ProductRequest(
                "SKU-NEW",
                "5901234569999",
                "Új Termék",
                "Új leírás",
                BigDecimal.valueOf(2.5),
                BigDecimal.valueOf(0.003),
                false,
                ZoneType.NORMAL,
                5,
                true
        );

        ProductResponse created = facade.createProduct(req);
        assertThat(created.id()).isNotNull();
        assertThat(created.sku()).isEqualTo("SKU-NEW");
        assertThat(facade.getProductById(created.id())).isEqualTo(created);
    }

    @Test
    @DisplayName("Should update existing product or throw if not found")
    void testUpdateProduct() {
        ProductRequest updateReq = new ProductRequest(
                "SKU-1092-UPDATED",
                "5901234567890",
                "Fékfolyadék Frissített",
                "Friss leírás",
                BigDecimal.valueOf(0.7),
                BigDecimal.valueOf(0.001),
                true,
                ZoneType.HAZARDOUS,
                12,
                false
        );

        ProductResponse updated = facade.updateProduct(1L, updateReq);
        assertThat(updated.sku()).isEqualTo("SKU-1092-UPDATED");
        assertThat(updated.name()).isEqualTo("Fékfolyadék Frissített");
        assertThat(updated.active()).isFalse();

        assertThatThrownBy(() -> facade.updateProduct(999L, updateReq))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
