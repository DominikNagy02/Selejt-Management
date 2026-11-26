package hu.unideb.inf.backend.service.masterdata;

import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.model.entity.ZoneEntity;
import hu.unideb.inf.backend.repository.ShelfRepository;
import hu.unideb.inf.backend.repository.ZoneRepository;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.ShelfRequest;
import hu.unideb.inf.core.dto.ShelfResponse;
import hu.unideb.inf.core.exception.CapacityViolation;
import hu.unideb.inf.core.exception.DuplicateResourceException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ShelfService {

    private static final String RESOURCE_TYPE = "Shelf";
    private static final String ZONE_RESOURCE_TYPE = "Zone";

    private final ShelfRepository shelfRepository;
    private final ZoneRepository zoneRepository;

    public ShelfService(ShelfRepository shelfRepository, ZoneRepository zoneRepository) {
        this.shelfRepository = Objects.requireNonNull(
                shelfRepository,
                "shelfRepository must not be null"
        );
        this.zoneRepository = Objects.requireNonNull(
                zoneRepository,
                "zoneRepository must not be null"
        );
    }

    public PageResponse<ShelfResponse> findAll(
            Long warehouseId,
            Long zoneId,
            Boolean activeOnly,
            Pageable pageable
    ) {
        boolean onlyActive = Boolean.TRUE.equals(activeOnly);
        if (warehouseId == null && zoneId == null && !onlyActive && pageable != null && pageable.isPaged()) {
            return PageMapper.fromPage(shelfRepository.findAll(pageable).map(this::toResponse));
        }

        List<ShelfEntity> candidates;
        if (zoneId != null) {
            candidates = onlyActive
                    ? shelfRepository.findByZoneIdAndActiveTrue(zoneId)
                    : shelfRepository.findByZoneId(zoneId);
        } else if (warehouseId != null) {
            candidates = shelfRepository.findByZoneWarehouseId(warehouseId);
        } else {
            candidates = shelfRepository.findAll();
        }

        List<ShelfResponse> items = candidates.stream()
                .filter(shelf -> warehouseId == null || shelf.getZone().getWarehouse().getId().equals(warehouseId))
                .filter(shelf -> !onlyActive || shelf.isActive())
                .map(this::toResponse)
                .toList();
        return PageMapper.paginate(items, pageable);
    }

    public ShelfResponse findById(Long id) {
        return toResponse(getShelfOrThrow(id));
    }

    public ShelfResponse findByCode(String code) {
        Objects.requireNonNull(code, "code must not be null");
        String trimmed = code.trim();
        ShelfEntity shelf = shelfRepository.findByCode(trimmed)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE_TYPE, trimmed));
        return toResponse(shelf);
    }

    @Transactional
    public ShelfResponse create(ShelfRequest request) {
        validateDimensions(request);
        ZoneEntity zone = getZoneOrThrow(request.zoneId());
        String code = request.code().trim();
        if (shelfRepository.existsByCode(code)) {
            throw DuplicateResourceException.of(RESOURCE_TYPE, "code", code);
        }

        boolean active = request.active() == null || request.active();
        ShelfEntity entity = new ShelfEntity(
                zone,
                code,
                request.storageType(),
                request.maxTiers(),
                request.maxWeightKg(),
                request.maxVolumeM3(),
                active
        );
        return toResponse(shelfRepository.save(entity));
    }

    @Transactional
    public ShelfResponse update(Long id, ShelfRequest request) {
        validateDimensions(request);
        ShelfEntity entity = getShelfOrThrow(id);
        ZoneEntity zone = getZoneOrThrow(request.zoneId());
        String code = request.code().trim();
        if (!entity.getCode().equals(code) && shelfRepository.existsByCode(code)) {
            throw DuplicateResourceException.of(RESOURCE_TYPE, "code", code);
        }

        ensureMaxNotBelowCurrent(code, entity, request.maxWeightKg(), request.maxVolumeM3());

        entity.setZone(zone);
        entity.setCode(code);
        entity.setStorageType(request.storageType());
        entity.setMaxTiers(request.maxTiers());
        entity.setMaxWeightKg(request.maxWeightKg());
        entity.setMaxVolumeM3(request.maxVolumeM3());
        if (request.active() != null) {
            entity.setActive(request.active());
        }
        return toResponse(shelfRepository.save(entity));
    }

    @Transactional
    public ShelfResponse deactivate(Long id) {
        ShelfEntity entity = getShelfOrThrow(id);
        entity.setActive(false);
        return toResponse(shelfRepository.save(entity));
    }

    private void validateDimensions(ShelfRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(request.storageType(), "storageType must not be null");
        Objects.requireNonNull(request.maxWeightKg(), "maxWeightKg must not be null");
        Objects.requireNonNull(request.maxVolumeM3(), "maxVolumeM3 must not be null");
        if (request.maxTiers() < 1) {
            throw new IllegalArgumentException("maxTiers must be at least 1");
        }
        if (request.maxWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("maxWeightKg must be positive");
        }
        if (request.maxVolumeM3().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("maxVolumeM3 must be positive");
        }
    }

    private void ensureMaxNotBelowCurrent(
            String shelfCode,
            ShelfEntity entity,
            BigDecimal newMaxWeightKg,
            BigDecimal newMaxVolumeM3
    ) {
        List<CapacityViolation> violations = new ArrayList<>();
        if (newMaxWeightKg.compareTo(entity.getCurrentWeightKg()) < 0) {
            violations.add(new CapacityViolation(
                    "weight",
                    newMaxWeightKg,
                    entity.getCurrentWeightKg(),
                    "kg"
            ));
        }
        if (newMaxVolumeM3.compareTo(entity.getCurrentVolumeM3()) < 0) {
            violations.add(new CapacityViolation(
                    "volume",
                    newMaxVolumeM3,
                    entity.getCurrentVolumeM3(),
                    "m³"
            ));
        }
        if (!violations.isEmpty()) {
            throw new ShelfCapacityExceededException(shelfCode, violations);
        }
    }

    private ShelfEntity getShelfOrThrow(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return shelfRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE_TYPE, id));
    }

    private ZoneEntity getZoneOrThrow(Long zoneId) {
        Objects.requireNonNull(zoneId, "zoneId must not be null");
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> ResourceNotFoundException.of(ZONE_RESOURCE_TYPE, zoneId));
    }

    private ShelfResponse toResponse(ShelfEntity entity) {
        return new ShelfResponse(
                entity.getId(),
                entity.getZone().getId(),
                entity.getZone().getCode(),
                entity.getCode(),
                entity.getStorageType(),
                entity.getMaxTiers(),
                entity.getMaxWeightKg(),
                entity.getMaxVolumeM3(),
                entity.getCurrentWeightKg(),
                entity.getCurrentVolumeM3(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
