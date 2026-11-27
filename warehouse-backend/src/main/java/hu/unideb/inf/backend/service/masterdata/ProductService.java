package hu.unideb.inf.backend.service.masterdata;

import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.repository.ProductRepository;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.ProductRequest;
import hu.unideb.inf.core.dto.ProductResponse;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.DuplicateResourceException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final String RESOURCE_TYPE = "Product";

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = Objects.requireNonNull(
                productRepository,
                "productRepository must not be null"
        );
    }

    public PageResponse<ProductResponse> findAll(
            String search,
            ZoneType requiredZoneType,
            Boolean activeOnly,
            Pageable pageable
    ) {
        String normalizedSearch = normalizeSearch(search);
        boolean onlyActive = Boolean.TRUE.equals(activeOnly);
        if (normalizedSearch == null && requiredZoneType == null && !onlyActive
                && pageable != null && pageable.isPaged()) {
            return PageMapper.fromPage(productRepository.findAll(pageable).map(this::toResponse));
        }

        List<ProductEntity> candidates;
        if (requiredZoneType != null) {
            candidates = productRepository.findByRequiredZoneType(requiredZoneType);
        } else if (onlyActive) {
            candidates = productRepository.findAllByActiveTrue();
        } else {
            candidates = productRepository.findAll();
        }

        List<ProductResponse> items = candidates.stream()
                .filter(product -> !onlyActive || product.isActive())
                .filter(product -> matchesSearch(product, normalizedSearch))
                .map(this::toResponse)
                .toList();
        return PageMapper.paginate(items, pageable);
    }

    public ProductResponse findById(Long id) {
        return toResponse(getProductOrThrow(id));
    }

    public ProductResponse findBySku(String sku) {
        Objects.requireNonNull(sku, "sku must not be null");
        String trimmed = sku.trim();
        ProductEntity product = productRepository.findBySku(trimmed)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE_TYPE, trimmed));
        return toResponse(product);
    }

    public ProductResponse findByBarcode(String barcode) {
        Objects.requireNonNull(barcode, "barcode must not be null");
        String trimmed = barcode.trim();
        ProductEntity product = productRepository.findByBarcode(trimmed)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE_TYPE, trimmed));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String sku = request.sku().trim();
        String barcode = request.barcode().trim();
        if (productRepository.existsBySku(sku)) {
            throw DuplicateResourceException.of(RESOURCE_TYPE, "sku", sku);
        }
        if (productRepository.existsByBarcode(barcode)) {
            throw DuplicateResourceException.of(RESOURCE_TYPE, "barcode", barcode);
        }

        ZoneType zoneType = request.requiredZoneType() != null ? request.requiredZoneType() : ZoneType.NORMAL;
        boolean active = request.active() == null || request.active();
        ProductEntity entity = new ProductEntity(
                sku,
                barcode,
                request.name().trim(),
                normalizeOptionalText(request.description()),
                request.unitWeightKg(),
                request.unitVolumeM3(),
                zoneType,
                request.minStockLevel(),
                active
        );
        return toResponse(productRepository.save(entity));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ProductEntity entity = getProductOrThrow(id);
        String sku = request.sku().trim();
        String barcode = request.barcode().trim();
        if (!entity.getSku().equals(sku) && productRepository.existsBySku(sku)) {
            throw DuplicateResourceException.of(RESOURCE_TYPE, "sku", sku);
        }
        if (!entity.getBarcode().equals(barcode) && productRepository.existsByBarcode(barcode)) {
            throw DuplicateResourceException.of(RESOURCE_TYPE, "barcode", barcode);
        }

        entity.setSku(sku);
        entity.setBarcode(barcode);
        entity.setName(request.name().trim());
        entity.setDescription(normalizeOptionalText(request.description()));
        entity.setUnitWeightKg(request.unitWeightKg());
        entity.setUnitVolumeM3(request.unitVolumeM3());
        if (request.requiredZoneType() != null) {
            entity.setRequiredZoneType(request.requiredZoneType());
        }
        entity.setMinStockLevel(request.minStockLevel());
        if (request.active() != null) {
            entity.setActive(request.active());
        }
        return toResponse(productRepository.save(entity));
    }

    @Transactional
    public ProductResponse deactivate(Long id) {
        ProductEntity entity = getProductOrThrow(id);
        entity.setActive(false);
        return toResponse(productRepository.save(entity));
    }

    private ProductEntity getProductOrThrow(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE_TYPE, id));
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSearch(String search) {
        String trimmed = normalizeOptionalText(search);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private boolean matchesSearch(ProductEntity product, String normalizedSearch) {
        if (normalizedSearch == null) {
            return true;
        }
        return containsIgnoreCase(product.getSku(), normalizedSearch)
                || containsIgnoreCase(product.getBarcode(), normalizedSearch)
                || containsIgnoreCase(product.getName(), normalizedSearch);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private ProductResponse toResponse(ProductEntity entity) {
        return new ProductResponse(
                entity.getId(),
                entity.getSku(),
                entity.getBarcode(),
                entity.getName(),
                entity.getDescription(),
                entity.getUnitWeightKg(),
                entity.getUnitVolumeM3(),
                false,
                entity.getRequiredZoneType(),
                entity.getMinStockLevel(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
