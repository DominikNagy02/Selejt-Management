package hu.unideb.inf.backend.model.entity;

import hu.unideb.inf.core.enums.ReservationStatus;
import hu.unideb.inf.core.enums.ReservationType;
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
@Table(name = "stock_reservations")
public class StockReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItemEntity inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_line_id")
    private TaskLineEntity taskLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_order_line_id")
    private DeliveryOrderLineEntity deliveryOrderLine;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type", nullable = false, length = 20)
    private ReservationType reservationType;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    public StockReservationEntity() {
    }

    public StockReservationEntity(InventoryItemEntity inventoryItem, TaskLineEntity taskLine,
                                  DeliveryOrderLineEntity deliveryOrderLine, ReservationType reservationType,
                                  int quantity, ReservationStatus status, Instant expiresAt, Long createdBy) {
        this.inventoryItem = inventoryItem;
        this.taskLine = taskLine;
        this.deliveryOrderLine = deliveryOrderLine;
        this.reservationType = reservationType;
        this.quantity = quantity;
        this.status = status != null ? status : ReservationStatus.ACTIVE;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
    }

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = ReservationStatus.ACTIVE;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InventoryItemEntity getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItemEntity inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public TaskLineEntity getTaskLine() {
        return taskLine;
    }

    public void setTaskLine(TaskLineEntity taskLine) {
        this.taskLine = taskLine;
    }

    public DeliveryOrderLineEntity getDeliveryOrderLine() {
        return deliveryOrderLine;
    }

    public void setDeliveryOrderLine(DeliveryOrderLineEntity deliveryOrderLine) {
        this.deliveryOrderLine = deliveryOrderLine;
    }

    public ReservationType getReservationType() {
        return reservationType;
    }

    public void setReservationType(ReservationType reservationType) {
        this.reservationType = reservationType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockReservationEntity that = (StockReservationEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StockReservationEntity{" +
                "id=" + id +
                ", reservationType=" + reservationType +
                ", quantity=" + quantity +
                ", status=" + status +
                ", expiresAt=" + expiresAt +
                ", createdBy=" + createdBy +
                '}';
    }
}
