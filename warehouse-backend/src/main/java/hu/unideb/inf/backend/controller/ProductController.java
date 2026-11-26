package hu.unideb.inf.backend.controller;

import hu.unideb.inf.backend.service.masterdata.ProductService;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.ProductRequest;
import hu.unideb.inf.core.dto.ProductResponse;
import hu.unideb.inf.core.enums.ZoneType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = Objects.requireNonNull(
                productService,
                "productService must not be null"
        );
    }

    @GetMapping
    public PageResponse<ProductResponse> findAll(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "zoneType", required = false) ZoneType zoneType,
            @RequestParam(name = "activeOnly", required = false) Boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return productService.findAll(search, zoneType, activeOnly, pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable("id") Long id) {
        return productService.findById(id);
    }

    @GetMapping("/sku/{sku}")
    public ProductResponse findBySku(@PathVariable("sku") String sku) {
        return productService.findBySku(sku);
    }

    @GetMapping("/barcode/{barcode}")
    public ProductResponse findByBarcode(@PathVariable("barcode") String barcode) {
        return productService.findByBarcode(barcode);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public ProductResponse deactivate(@PathVariable("id") Long id) {
        return productService.deactivate(id);
    }
}
