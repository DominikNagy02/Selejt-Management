package hu.unideb.inf.backend.service.facade;

import hu.unideb.inf.backend.service.facade.dto.StockAlertDto;
import hu.unideb.inf.backend.service.facade.dto.ZoneLoadDto;
import hu.unideb.inf.core.dto.ProductRequest;
import hu.unideb.inf.core.dto.ProductResponse;
import hu.unideb.inf.core.enums.ZoneType;

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

    List<ProductResponse> getProducts(String query, ZoneType zoneType);

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);
}
