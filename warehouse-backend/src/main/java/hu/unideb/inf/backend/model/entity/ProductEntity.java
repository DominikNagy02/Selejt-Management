package hu.unideb.inf.backend.model.entity;

import hu.unideb.inf.backend.service.capacity.ItemSize;
import hu.unideb.inf.core.enums.ZoneType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, unique = true, length = 64)
    private String barcode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "unit_weight_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal unitWeightKg;

    @Column(name = "unit_volume_m3", nullable = false, precision = 12, scale = 6)
    private BigDecimal unitVolumeM3;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_zone_type", nullable = false, length = 20)
    private ZoneType requiredZoneType = ZoneType.NORMAL;

    @Column(name = "min_stock_level", nullable = false)
    private int minStockLevel = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProductEntity() {
    }

    public ProductEntity(String sku, String barcode, String name, String description,
                         BigDecimal unitWeightKg, BigDecimal unitVolumeM3,
                         ZoneType requiredZoneType, int minStockLevel, boolean active) {
        this.sku = sku;
        this.barcode = barcode;
        this.name = name;
        this.description = description;
        this.unitWeightKg = unitWeightKg;
        this.unitVolumeM3 = unitVolumeM3;
        this.requiredZoneType = requiredZoneType != null ? requiredZoneType : ZoneType.NORMAL;
        this.minStockLevel = minStockLevel;
        this.active = active;
    }

    public ItemSize toItemSize() {
        return new ItemSize(unitWeightKg, unitVolumeM3);
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.requiredZoneType == null) {
            this.requiredZoneType = ZoneType.NORMAL;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getUnitWeightKg() {
        return unitWeightKg;
    }

    public void setUnitWeightKg(BigDecimal unitWeightKg) {
        this.unitWeightKg = unitWeightKg;
    }

    public BigDecimal getUnitVolumeM3() {
        return unitVolumeM3;
    }

    public void setUnitVolumeM3(BigDecimal unitVolumeM3) {
        this.unitVolumeM3 = unitVolumeM3;
    }

    public ZoneType getRequiredZoneType() {
        return requiredZoneType;
    }

    public void setRequiredZoneType(ZoneType requiredZoneType) {
        this.requiredZoneType = requiredZoneType;
    }

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(int minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductEntity that = (ProductEntity) o;
        return Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }

    @Override
    public String toString() {
        return "ProductEntity{" +
                "id=" + id +
                ", sku='" + sku + '\'' +
                ", barcode='" + barcode + '\'' +
                ", name='" + name + '\'' +
                ", requiredZoneType=" + requiredZoneType +
                ", active=" + active +
                ", version=" + version +
                '}';
    }
}
