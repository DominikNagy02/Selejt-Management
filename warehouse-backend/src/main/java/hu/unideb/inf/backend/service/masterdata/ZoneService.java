package hu.unideb.inf.backend.service.masterdata;

import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import hu.unideb.inf.backend.model.entity.ZoneEntity;
import hu.unideb.inf.backend.repository.WarehouseRepository;
import hu.unideb.inf.backend.repository.ZoneRepository;
import hu.unideb.inf.core.dto.ZoneRequest;
import hu.unideb.inf.core.dto.ZoneResponse;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.DuplicateResourceException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ZoneService {

    private static final String RESOURCE_TYPE = "Zone";
    private static final String WAREHOUSE_RESOURCE_TYPE = "Warehouse";

    private final ZoneRepository zoneRepository;
    private final WarehouseRepository warehouseRepository;

    public ZoneService(
            ZoneRepository zoneRepository,
            WarehouseRepository warehouseRepository
    ) {
        this.zoneRepository = Objects.requireNonNull(
                zoneRepository,
                "zoneRepository must not be null"
        );
        this.warehouseRepository = Objects.requireNonNull(
                warehouseRepository,
                "warehouseRepository must not be null"
        );
    }

    public List<ZoneResponse> findAll(Long warehouseId, ZoneType zoneType, Boolean activeOnly) {
        boolean onlyActive = Boolean.TRUE.equals(activeOnly);
        List<ZoneEntity> candidates;
        if (warehouseId != null) {
            candidates = onlyActive
                    ? zoneRepository.findByWarehouseIdAndActiveTrue(warehouseId)
                    : zoneRepository.findByWarehouseId(warehouseId);
        } else if (zoneType != null && onlyActive) {
            candidates = zoneRepository.findByZoneTypeAndActiveTrue(zoneType);
        } else {
            candidates = zoneRepository.findAll();
        }

        return candidates.stream()
                .filter(zone -> zoneType == null || zone.getZoneType() == zoneType)
                .filter(zone -> !onlyActive || zone.isActive())
                .map(this::toResponse)
                .toList();
    }

    public ZoneResponse findById(Long id) {
        return toResponse(getZoneOrThrow(id));
    }

    @Transactional
    public ZoneResponse create(ZoneRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        WarehouseEntity warehouse = getWarehouseOrThrow(request.warehouseId());
        String code = request.code().trim();
        if (zoneRepository.existsByWarehouseIdAndCode(warehouse.getId(), code)) {
            throw DuplicateResourceException.of(RESOURCE_TYPE, "code", code);
        }

        boolean active = request.active() == null || request.active();
        ZoneEntity entity = new ZoneEntity(
                warehouse,
                code,
                request.name().trim(),
                request.zoneType(),
                active
        );
        return toResponse(zoneRepository.save(entity));
    }

    @Transactional
    public ZoneResponse update(Long id, ZoneRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ZoneEntity entity = getZoneOrThrow(id);
        WarehouseEntity warehouse = getWarehouseOrThrow(request.warehouseId());
        String code = request.code().trim();

        boolean keyChanged = !entity.getWarehouse().getId().equals(warehouse.getId())
                || !entity.getCode().equals(code);
        if (keyChanged && zoneRepository.existsByWarehouseIdAndCode(warehouse.getId(), code)) {
            throw DuplicateResourceException.of(RESOURCE_TYPE, "code", code);
        }

        entity.setWarehouse(warehouse);
        entity.setCode(code);
        entity.setName(request.name().trim());
        entity.setZoneType(request.zoneType());
        if (request.active() != null) {
            entity.setActive(request.active());
        }
        return toResponse(zoneRepository.save(entity));
    }

    @Transactional
    public ZoneResponse deactivate(Long id) {
        ZoneEntity entity = getZoneOrThrow(id);
        entity.setActive(false);
        return toResponse(zoneRepository.save(entity));
    }

    private ZoneEntity getZoneOrThrow(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return zoneRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE_TYPE, id));
    }

    private WarehouseEntity getWarehouseOrThrow(Long warehouseId) {
        Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> ResourceNotFoundException.of(WAREHOUSE_RESOURCE_TYPE, warehouseId));
    }

    private ZoneResponse toResponse(ZoneEntity entity) {
        return new ZoneResponse(
                entity.getId(),
                entity.getWarehouse().getId(),
                entity.getCode(),
                entity.getName(),
                entity.getZoneType(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
