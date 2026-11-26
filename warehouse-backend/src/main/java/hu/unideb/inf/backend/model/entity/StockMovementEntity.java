package hu.unideb.inf.backend.model.entity;

import hu.unideb.inf.core.enums.MovementType;
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
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "stock_movements")
public class StockMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private LotEntity lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItemEntity inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_shelf_id")
    private ShelfEntity fromShelf;

    @Column(name = "from_tier_level")
    private Integer fromTierLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_shelf_id")
    private ShelfEntity toShelf;

    @Column(name = "to_tier_level")
    private Integer toTierLevel;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private TaskEntity task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_order_id")
    private DeliveryOrderEntity deliveryOrder;

    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StockMovementEntity() {
    }

    public StockMovementEntity(MovementType movementType, LotEntity lot, InventoryItemEntity inventoryItem,
                               ShelfEntity fromShelf, Integer fromTierLevel,
                               ShelfEntity toShelf, Integer toTierLevel,
                               int quantity, String reason, TaskEntity task,
                               DeliveryOrderEntity deliveryOrder, Long performedBy) {
        this.movementType = movementType;
        this.lot = lot;
        this.inventoryItem = inventoryItem;
        this.fromShelf = fromShelf;
        this.fromTierLevel = fromTierLevel;
        this.toShelf = toShelf;
        this.toTierLevel = toTierLevel;
        this.quantity = quantity;
        this.reason = reason;
        this.task = task;
        this.deliveryOrder = deliveryOrder;
        this.performedBy = performedBy;
    }

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public LotEntity getLot() {
        return lot;
    }

    public void setLot(LotEntity lot) {
        this.lot = lot;
    }

    public InventoryItemEntity getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItemEntity inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public ShelfEntity getFromShelf() {
        return fromShelf;
    }

    public void setFromShelf(ShelfEntity fromShelf) {
        this.fromShelf = fromShelf;
    }

    public Integer getFromTierLevel() {
        return fromTierLevel;
    }

    public void setFromTierLevel(Integer fromTierLevel) {
        this.fromTierLevel = fromTierLevel;
    }

    public ShelfEntity getToShelf() {
        return toShelf;
    }

    public void setToShelf(ShelfEntity toShelf) {
        this.toShelf = toShelf;
    }

    public Integer getToTierLevel() {
        return toTierLevel;
    }

    public void setToTierLevel(Integer toTierLevel) {
        this.toTierLevel = toTierLevel;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public TaskEntity getTask() {
        return task;
    }

    public void setTask(TaskEntity task) {
        this.task = task;
    }

    public DeliveryOrderEntity getDeliveryOrder() {
        return deliveryOrder;
    }

    public void setDeliveryOrder(DeliveryOrderEntity deliveryOrder) {
        this.deliveryOrder = deliveryOrder;
    }

    public Long getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(Long performedBy) {
        this.performedBy = performedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockMovementEntity that = (StockMovementEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StockMovementEntity{" +
                "id=" + id +
                ", movementType=" + movementType +
                ", quantity=" + quantity +
                ", performedBy=" + performedBy +
                ", createdAt=" + createdAt +
                '}';
    }
}
