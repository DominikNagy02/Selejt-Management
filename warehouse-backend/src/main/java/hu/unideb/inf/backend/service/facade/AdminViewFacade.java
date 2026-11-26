package hu.unideb.inf.backend.service.facade;

import hu.unideb.inf.backend.service.facade.dto.StockAlertDto;
import hu.unideb.inf.backend.service.facade.dto.ZoneLoadDto;

import java.util.List;

public interface AdminViewFacade {

    int getTotalProducts();

    String getOccupancyRate();

    int getCriticalStockCount();

    int getActiveTasksCount();

    List<ZoneLoadDto> getZoneLoads();

    List<StockAlertDto> getStockAlerts();

    String getCurrentUsername();

    String getCurrentUserRole();
}
