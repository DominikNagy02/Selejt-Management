package hu.unideb.inf.backend.service.masterdata;

import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import hu.unideb.inf.backend.repository.WarehouseRepository;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.WarehouseRequest;
import hu.unideb.inf.core.dto.WarehouseResponse;
import hu.unideb.inf.core.exception.DuplicateResourceException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class WarehouseService {

    private static final String RESOURCE_TYPE = "Warehouse";

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = Objects.requireNonNull(
                warehouseRepository,
                "warehouseRepository must not be null"
        );
    }

    public PageResponse<WarehouseResponse> findAll(Boolean activeOnly, Pageable pageable) {
        boolean onlyActive = Boolean.TRUE.equals(activeOnly);
        if (!onlyActive && pageable != null && pageable.isPaged()) {
            return PageMapper.fromPage(warehouseRepository.findAll(pageable).map(this::toResponse));
        }
        List<WarehouseResponse> items = (onlyActive
                ? warehouseRepository.findAllByActiveTrue()
                : warehouseRepository.findAll())
                .stream()
                .map(this::toResponse)
                .toList();
        return PageMapper.paginate(items, pageable);
    }

    public WarehouseResponse findById(Long id) {
        return toResponse(getEntityOrThrow(id));
    }

    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String code = request.code().trim();
        if (warehouseRepository.existsByCode(code)) {
            throw DuplicateResourceException.of(RESOURCE_TYPE, "code", code);
        }

        boolean active = request.active() == null || request.active();
        WarehouseEntity entity = new WarehouseEntity(
                code,
                request.name().trim(),
                normalizeOptionalText(request.address()),
                active
        );
        return toResponse(warehouseRepository.save(entity));
    }

    @Transactional
    public WarehouseResponse update(Long id, WarehouseRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        WarehouseEntity entity = getEntityOrThrow(id);
        String code = request.code().trim();
        if (!entity.getCode().equals(code) && warehouseRepository.existsByCode(code)) {
            throw DuplicateResourceException.of(RESOURCE_TYPE, "code", code);
        }

        entity.setCode(code);
        entity.setName(request.name().trim());
        entity.setAddress(normalizeOptionalText(request.address()));
        if (request.active() != null) {
            entity.setActive(request.active());
        }
        return toResponse(warehouseRepository.save(entity));
    }

    @Transactional
    public WarehouseResponse deactivate(Long id) {
        WarehouseEntity entity = getEntityOrThrow(id);
        entity.setActive(false);
        return toResponse(warehouseRepository.save(entity));
    }

    private WarehouseEntity getEntityOrThrow(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return warehouseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE_TYPE, id));
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private WarehouseResponse toResponse(WarehouseEntity entity) {
        return new WarehouseResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getAddress(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
