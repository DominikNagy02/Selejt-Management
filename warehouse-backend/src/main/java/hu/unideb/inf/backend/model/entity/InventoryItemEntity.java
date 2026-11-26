package hu.unideb.inf.backend.model.entity;

import hu.unideb.inf.backend.service.picking.StockLot;
import hu.unideb.inf.core.enums.InventoryStatus;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "inventory_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_inv_shelf_lot_tier", columnNames = {"shelf_id", "lot_id", "tier_level"})
        }
)
public class InventoryItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelf_id", nullable = false)
    private ShelfEntity shelf;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private LotEntity lot;

    @Column(name = "tier_level", nullable = false)
    private int tierLevel = 0;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryStatus status = InventoryStatus.STORED;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InventoryItemEntity() {
    }

    public InventoryItemEntity(ShelfEntity shelf, LotEntity lot, int tierLevel, int quantity,
                               int reservedQuantity, InventoryStatus status, Instant receivedAt) {
        this.shelf = shelf;
        this.lot = lot;
        this.tierLevel = tierLevel;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.status = status != null ? status : InventoryStatus.STORED;
        this.receivedAt = receivedAt != null ? receivedAt : Instant.now();
    }

    public int getAvailableQuantity() {
        if (status != InventoryStatus.STORED) {
            return 0;
        }
        return Math.max(0, quantity - reservedQuantity);
    }

    public StockLot toStockLot() {
        return new StockLot(
                id,
                shelf != null ? shelf.getCode() : null,
                tierLevel,
                lot != null ? lot.getLotNumber() : null,
                getAvailableQuantity(),
                receivedAt != null ? receivedAt : Instant.now(),
                lot != null ? lot.getExpirationDate() : null
        );
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (this.receivedAt == null) {
            this.receivedAt = now;
        }
        this.updatedAt = now;
        if (this.status == null) {
            this.status = InventoryStatus.STORED;
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

    public ShelfEntity getShelf() {
        return shelf;
    }

    public void setShelf(ShelfEntity shelf) {
        this.shelf = shelf;
    }

    public LotEntity getLot() {
        return lot;
    }

    public void setLot(LotEntity lot) {
        this.lot = lot;
    }

    public int getTierLevel() {
        return tierLevel;
    }

    public void setTierLevel(int tierLevel) {
        this.tierLevel = tierLevel;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(int reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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
        InventoryItemEntity that = (InventoryItemEntity) o;
        return tierLevel == that.tierLevel &&
                Objects.equals(shelf, that.shelf) &&
                Objects.equals(lot, that.lot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shelf, lot, tierLevel);
    }

    @Override
    public String toString() {
        return "InventoryItemEntity{" +
                "id=" + id +
                ", tierLevel=" + tierLevel +
                ", quantity=" + quantity +
                ", reservedQuantity=" + reservedQuantity +
                ", status=" + status +
                ", version=" + version +
                '}';
    }
}
