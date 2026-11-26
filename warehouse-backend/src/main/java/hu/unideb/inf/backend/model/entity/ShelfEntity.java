package hu.unideb.inf.backend.model.entity;

import hu.unideb.inf.backend.service.capacity.ShelfLoad;
import hu.unideb.inf.core.enums.StorageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "shelves")
public class ShelfEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private ZoneEntity zone;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 20)
    private StorageType storageType = StorageType.RACK;

    @Column(name = "max_tiers", nullable = false)
    private int maxTiers = 1;

    @Column(name = "max_weight_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal maxWeightKg;

    @Column(name = "max_volume_m3", nullable = false, precision = 12, scale = 6)
    private BigDecimal maxVolumeM3;

    @Column(name = "current_weight_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal currentWeightKg = BigDecimal.ZERO;

    @Column(name = "current_volume_m3", nullable = false, precision = 12, scale = 6)
    private BigDecimal currentVolumeM3 = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ShelfEntity() {
    }

    public ShelfEntity(ZoneEntity zone, String code, StorageType storageType, int maxTiers,
                       BigDecimal maxWeightKg, BigDecimal maxVolumeM3, boolean active) {
        this.zone = zone;
        this.code = code;
        this.storageType = storageType != null ? storageType : StorageType.RACK;
        this.maxTiers = maxTiers;
        this.maxWeightKg = maxWeightKg;
        this.maxVolumeM3 = maxVolumeM3;
        this.currentWeightKg = BigDecimal.ZERO;
        this.currentVolumeM3 = BigDecimal.ZERO;
        this.active = active;
    }

    public ShelfLoad toShelfLoad() {
        return new ShelfLoad(code, currentWeightKg, currentVolumeM3, maxWeightKg, maxVolumeM3);
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.currentWeightKg == null) {
            this.currentWeightKg = BigDecimal.ZERO;
        }
        if (this.currentVolumeM3 == null) {
            this.currentVolumeM3 = BigDecimal.ZERO;
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

    public ZoneEntity getZone() {
        return zone;
    }

    public void setZone(ZoneEntity zone) {
        this.zone = zone;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public int getMaxTiers() {
        return maxTiers;
    }

    public void setMaxTiers(int maxTiers) {
        this.maxTiers = maxTiers;
    }

    public BigDecimal getMaxWeightKg() {
        return maxWeightKg;
    }

    public void setMaxWeightKg(BigDecimal maxWeightKg) {
        this.maxWeightKg = maxWeightKg;
    }

    public BigDecimal getMaxVolumeM3() {
        return maxVolumeM3;
    }

    public void setMaxVolumeM3(BigDecimal maxVolumeM3) {
        this.maxVolumeM3 = maxVolumeM3;
    }

    public BigDecimal getCurrentWeightKg() {
        return currentWeightKg;
    }

    public void setCurrentWeightKg(BigDecimal currentWeightKg) {
        this.currentWeightKg = currentWeightKg;
    }

    public BigDecimal getCurrentVolumeM3() {
        return currentVolumeM3;
    }

    public void setCurrentVolumeM3(BigDecimal currentVolumeM3) {
        this.currentVolumeM3 = currentVolumeM3;
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
        ShelfEntity that = (ShelfEntity) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "ShelfEntity{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", storageType=" + storageType +
                ", maxTiers=" + maxTiers +
                ", maxWeightKg=" + maxWeightKg +
                ", maxVolumeM3=" + maxVolumeM3 +
                ", currentWeightKg=" + currentWeightKg +
                ", currentVolumeM3=" + currentVolumeM3 +
                ", active=" + active +
                ", version=" + version +
                '}';
    }
}
