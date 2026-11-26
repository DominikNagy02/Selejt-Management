package hu.unideb.inf.backend.service.facade;

import hu.unideb.inf.backend.service.facade.dto.StockAlertDto;
import hu.unideb.inf.backend.service.facade.dto.ZoneLoadDto;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Override
    public int getTotalProducts() {
        return 142;
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
}
