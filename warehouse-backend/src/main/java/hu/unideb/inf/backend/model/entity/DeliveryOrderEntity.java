package hu.unideb.inf.backend.model.entity;

import hu.unideb.inf.core.enums.DeliveryOrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "delivery_orders")
public class DeliveryOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "do_number", nullable = false, unique = true, length = 30)
    private String doNumber;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryOrderStatus status = DeliveryOrderStatus.OPEN;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "deliveryOrder", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<DeliveryOrderLineEntity> lines = new ArrayList<>();

    public DeliveryOrderEntity() {
    }

    public DeliveryOrderEntity(String doNumber, String customerName, DeliveryOrderStatus status,
                               LocalDate dueDate, Long createdBy) {
        this.doNumber = doNumber;
        this.customerName = customerName;
        this.status = status != null ? status : DeliveryOrderStatus.OPEN;
        this.dueDate = dueDate;
        this.createdBy = createdBy;
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.status == null) {
            this.status = DeliveryOrderStatus.OPEN;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addLine(DeliveryOrderLineEntity line) {
        lines.add(line);
        line.setDeliveryOrder(this);
    }

    public void removeLine(DeliveryOrderLineEntity line) {
        lines.remove(line);
        line.setDeliveryOrder(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDoNumber() {
        return doNumber;
    }

    public void setDoNumber(String doNumber) {
        this.doNumber = doNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public DeliveryOrderStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryOrderStatus status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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

    public List<DeliveryOrderLineEntity> getLines() {
        return lines;
    }

    public void setLines(List<DeliveryOrderLineEntity> lines) {
        this.lines = lines;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryOrderEntity that = (DeliveryOrderEntity) o;
        return Objects.equals(doNumber, that.doNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(doNumber);
    }

    @Override
    public String toString() {
        return "DeliveryOrderEntity{" +
                "id=" + id +
                ", doNumber='" + doNumber + '\'' +
                ", customerName='" + customerName + '\'' +
                ", status=" + status +
                ", dueDate=" + dueDate +
                ", version=" + version +
                '}';
    }
}
