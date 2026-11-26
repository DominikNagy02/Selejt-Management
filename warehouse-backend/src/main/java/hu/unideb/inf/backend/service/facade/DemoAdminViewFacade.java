package hu.unideb.inf.backend.service.facade;

import hu.unideb.inf.backend.service.facade.dto.StockAlertDto;
import hu.unideb.inf.backend.service.facade.dto.ZoneLoadDto;
import hu.unideb.inf.core.dto.ProductRequest;
import hu.unideb.inf.core.dto.ProductResponse;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DemoAdminViewFacade implements AdminViewFacade {

    private final List<ZoneLoadDto> zoneLoads = List.of(
            new ZoneLoadDto("A-Zóna (Száraz raktár - Normál)", 4500, 6000, 75, false, "bg-primary", "text-muted"),
            new ZoneLoadDto("B-Zóna (Hűtött raktár)", 1920, 2000, 96, true, "bg-danger", "text-danger fw-bold"),
            new ZoneLoadDto("C-Zóna (Veszélyes anyagok)", 320, 1500, 21, false, "bg-success", "text-muted")
    );

    private final List<StockAlertDto> stockAlerts = List.of(
            new StockAlertDto("Fékfolyadék DOT4 (SKU-1092)", "Készlet: 3 db (Min: 10 db)", "danger", null, "Rendelés"),
            new StockAlertDto("Szintetikus Motorolaj 5W30 (Lot-44)", "Lejár: 14 napon belül", "warning", "Polc: A-02-1", null)
    );

    private final List<ProductResponse> products = new CopyOnWriteArrayList<>(List.of(
            new ProductResponse(
                    1L,
                    "SKU-1092",
                    "5901234567890",
                    "Fékfolyadék DOT4 500ml",
                    "Nagy teljesítményű szintetikus fékfolyadék",
                    BigDecimal.valueOf(0.600).setScale(3),
                    BigDecimal.valueOf(0.000800).setScale(6),
                    true,
                    ZoneType.HAZARDOUS,
                    10,
                    true,
                    Instant.parse("2026-10-01T10:00:00Z"),
                    Instant.parse("2026-10-01T10:00:00Z")
            ),
            new ProductResponse(
                    2L,
                    "SKU-2044",
                    "5901234567891",
                    "Szintetikus Motorolaj 5W30 4L",
                    "Prémium minőségű motorolaj személygépkocsikhoz",
                    BigDecimal.valueOf(3.800).setScale(3),
                    BigDecimal.valueOf(0.004500).setScale(6),
                    false,
                    ZoneType.NORMAL,
                    20,
                    true,
                    Instant.parse("2026-10-02T11:00:00Z"),
                    Instant.parse("2026-10-02T11:00:00Z")
            ),
            new ProductResponse(
                    3L,
                    "SKU-3011",
                    "5901234567892",
                    "Hűtőfolyadék Koncentrátum -35C 1L",
                    "Alumíniumbarát fagyálló hűtőfolyadék",
                    BigDecimal.valueOf(1.150).setScale(3),
                    BigDecimal.valueOf(0.001200).setScale(6),
                    false,
                    ZoneType.CHILLED,
                    15,
                    true,
                    Instant.parse("2026-10-03T09:30:00Z"),
                    Instant.parse("2026-10-03T09:30:00Z")
            ),
            new ProductResponse(
                    4L,
                    "SKU-4050",
                    "5901234567893",
                    "Szélvédőmosó Folyadék Téli -20C 5L",
                    "Dér- és jégoldó szélvédőmosó",
                    BigDecimal.valueOf(5.000).setScale(3),
                    BigDecimal.valueOf(0.006000).setScale(6),
                    false,
                    ZoneType.NORMAL,
                    30,
                    false,
                    Instant.parse("2026-10-04T14:15:00Z"),
                    Instant.parse("2026-10-04T14:15:00Z")
            )
    ));

    private final AtomicLong nextId = new AtomicLong(5L);

    @Override
    public int getTotalProducts() {
        return products.size();
    }

    @Override
    public String getOccupancyRate() {
        return "78%";
    }

    @Override
    public int getCriticalStockCount() {
        return 5;
    }

    @Override
    public int getActiveTasksCount() {
        return 12;
    }

    @Override
    public List<ZoneLoadDto> getZoneLoads() {
        return zoneLoads;
    }

    @Override
    public List<StockAlertDto> getStockAlerts() {
        return stockAlerts;
    }

    @Override
    public String getCurrentUsername() {
        return "Adminisztrátor";
    }

    @Override
    public String getCurrentUserRole() {
        return "Diszpécser";
    }

    @Override
    public List<ProductResponse> getProducts(String query, ZoneType zoneType) {
        String cleanQuery = (query != null) ? query.trim().toLowerCase(Locale.ROOT) : "";
        return products.stream()
                .filter(p -> zoneType == null || p.requiredZoneType() == zoneType)
                .filter(p -> cleanQuery.isEmpty()
                        || p.sku().toLowerCase(Locale.ROOT).contains(cleanQuery)
                        || p.name().toLowerCase(Locale.ROOT).contains(cleanQuery)
                        || p.barcode().toLowerCase(Locale.ROOT).contains(cleanQuery))
                .toList();
    }

    @Override
    public ProductResponse getProductById(Long id) {
        return products.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Instant now = Instant.now();
        ProductResponse newProduct = new ProductResponse(
                nextId.getAndIncrement(),
                request.sku(),
                request.barcode(),
                request.name(),
                request.description(),
                request.unitWeightKg(),
                request.unitVolumeM3(),
                request.requiresExpiry(),
                request.requiredZoneType(),
                request.minStockLevel(),
                request.active() != null ? request.active() : true,
                now,
                now
        );
        products.add(newProduct);
        return newProduct;
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        for (int i = 0; i < products.size(); i++) {
            ProductResponse existing = products.get(i);
            if (existing.id().equals(id)) {
                ProductResponse updated = new ProductResponse(
                        id,
                        request.sku(),
                        request.barcode(),
                        request.name(),
                        request.description(),
                        request.unitWeightKg(),
                        request.unitVolumeM3(),
                        request.requiresExpiry(),
                        request.requiredZoneType(),
                        request.minStockLevel(),
                        request.active() != null ? request.active() : existing.active(),
                        existing.createdAt(),
                        Instant.now()
                );
                products.set(i, updated);
                return updated;
            }
        }
        throw ResourceNotFoundException.of("Product", id);
    }
}
