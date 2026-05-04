package dev.edgesecura.shoppingList.userdata.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shopping_lists", schema = "catalog")
public class ShoppingListEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShoppingListStatus status;

    @Column
    private BigDecimal total;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Transient
    private boolean isNew = true;

    public Long getUserId() { return userId; }
    public ShoppingListStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setId(UUID id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setStatus(ShoppingListStatus status) { this.status = status; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }
}
