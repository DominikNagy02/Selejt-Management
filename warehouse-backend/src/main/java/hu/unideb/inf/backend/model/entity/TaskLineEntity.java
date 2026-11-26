package hu.unideb.inf.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

@Entity
@Table(
        name = "task_lines",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_task_lines_task_line", columnNames = {"task_id", "line_no"})
        }
)
public class TaskLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity task;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private LotEntity lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_item_id")
    private InventoryItemEntity sourceItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_shelf_id")
    private ShelfEntity sourceShelf;

    @Column(name = "source_tier_level")
    private Integer sourceTierLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_shelf_id")
    private ShelfEntity targetShelf;

    @Column(name = "target_tier_level")
    private Integer targetTierLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_order_line_id")
    private DeliveryOrderLineEntity deliveryOrderLine;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "confirmed_quantity", nullable = false)
    private int confirmedQuantity = 0;

    public TaskLineEntity() {
    }

    public TaskLineEntity(TaskEntity task, int lineNo, LotEntity lot, InventoryItemEntity sourceItem,
                          ShelfEntity sourceShelf, Integer sourceTierLevel,
                          ShelfEntity targetShelf, Integer targetTierLevel,
                          DeliveryOrderLineEntity deliveryOrderLine, int quantity, int confirmedQuantity) {
        this.task = task;
        this.lineNo = lineNo;
        this.lot = lot;
        this.sourceItem = sourceItem;
        this.sourceShelf = sourceShelf;
        this.sourceTierLevel = sourceTierLevel;
        this.targetShelf = targetShelf;
        this.targetTierLevel = targetTierLevel;
        this.deliveryOrderLine = deliveryOrderLine;
        this.quantity = quantity;
        this.confirmedQuantity = confirmedQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TaskEntity getTask() {
        return task;
    }

    public void setTask(TaskEntity task) {
        this.task = task;
    }

    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    public LotEntity getLot() {
        return lot;
    }

    public void setLot(LotEntity lot) {
        this.lot = lot;
    }

    public InventoryItemEntity getSourceItem() {
        return sourceItem;
    }

    public void setSourceItem(InventoryItemEntity sourceItem) {
        this.sourceItem = sourceItem;
    }

    public ShelfEntity getSourceShelf() {
        return sourceShelf;
    }

    public void setSourceShelf(ShelfEntity sourceShelf) {
        this.sourceShelf = sourceShelf;
    }

    public Integer getSourceTierLevel() {
        return sourceTierLevel;
    }

    public void setSourceTierLevel(Integer sourceTierLevel) {
        this.sourceTierLevel = sourceTierLevel;
    }

    public ShelfEntity getTargetShelf() {
        return targetShelf;
    }

    public void setTargetShelf(ShelfEntity targetShelf) {
        this.targetShelf = targetShelf;
    }

    public Integer getTargetTierLevel() {
        return targetTierLevel;
    }

    public void setTargetTierLevel(Integer targetTierLevel) {
        this.targetTierLevel = targetTierLevel;
    }

    public DeliveryOrderLineEntity getDeliveryOrderLine() {
        return deliveryOrderLine;
    }

    public void setDeliveryOrderLine(DeliveryOrderLineEntity deliveryOrderLine) {
        this.deliveryOrderLine = deliveryOrderLine;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getConfirmedQuantity() {
        return confirmedQuantity;
    }

    public void setConfirmedQuantity(int confirmedQuantity) {
        this.confirmedQuantity = confirmedQuantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskLineEntity that = (TaskLineEntity) o;
        return lineNo == that.lineNo &&
                Objects.equals(task, that.task);
    }

    @Override
    public int hashCode() {
        return Objects.hash(task, lineNo);
    }

    @Override
    public String toString() {
        return "TaskLineEntity{" +
                "id=" + id +
                ", lineNo=" + lineNo +
                ", quantity=" + quantity +
                ", confirmedQuantity=" + confirmedQuantity +
                '}';
    }
}
