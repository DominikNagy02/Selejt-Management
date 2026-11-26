package hu.unideb.inf.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(
        name = "lots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_lots_product_lot_number", columnNames = {"product_id", "lot_number"})
        }
)
public class LotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "lot_number", nullable = false, length = 50)
    private String lotNumber;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LotEntity() {
    }

    public LotEntity(ProductEntity product, String lotNumber, LocalDate expirationDate) {
        this.product = product;
        this.lotNumber = lotNumber;
        this.expirationDate = expirationDate;
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

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
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
        LotEntity that = (LotEntity) o;
        return Objects.equals(lotNumber, that.lotNumber) &&
                Objects.equals(product, that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, lotNumber);
    }

    @Override
    public String toString() {
        return "LotEntity{" +
                "id=" + id +
                ", lotNumber='" + lotNumber + '\'' +
                ", expirationDate=" + expirationDate +
                '}';
    }
}
