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
        name = "delivery_order_lines",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_do_lines_order_line", columnNames = {"delivery_order_id", "line_no"})
        }
)
public class DeliveryOrderLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_order_id", nullable = false)
    private DeliveryOrderEntity deliveryOrder;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_lot_id")
    private LotEntity requestedLot;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "picked_quantity", nullable = false)
    private int pickedQuantity = 0;

    public DeliveryOrderLineEntity() {
    }

    public DeliveryOrderLineEntity(DeliveryOrderEntity deliveryOrder, int lineNo, ProductEntity product,
                                   LotEntity requestedLot, int quantity, int pickedQuantity) {
        this.deliveryOrder = deliveryOrder;
        this.lineNo = lineNo;
        this.product = product;
        this.requestedLot = requestedLot;
        this.quantity = quantity;
        this.pickedQuantity = pickedQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DeliveryOrderEntity getDeliveryOrder() {
        return deliveryOrder;
    }

    public void setDeliveryOrder(DeliveryOrderEntity deliveryOrder) {
        this.deliveryOrder = deliveryOrder;
    }

    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public LotEntity getRequestedLot() {
        return requestedLot;
    }

    public void setRequestedLot(LotEntity requestedLot) {
        this.requestedLot = requestedLot;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPickedQuantity() {
        return pickedQuantity;
    }

    public void setPickedQuantity(int pickedQuantity) {
        this.pickedQuantity = pickedQuantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryOrderLineEntity that = (DeliveryOrderLineEntity) o;
        return lineNo == that.lineNo &&
                Objects.equals(deliveryOrder, that.deliveryOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deliveryOrder, lineNo);
    }

    @Override
    public String toString() {
        return "DeliveryOrderLineEntity{" +
                "id=" + id +
                ", lineNo=" + lineNo +
                ", quantity=" + quantity +
                ", pickedQuantity=" + pickedQuantity +
                '}';
    }
}
