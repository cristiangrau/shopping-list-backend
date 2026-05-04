package dev.edgesecura.shoppingList.catalog.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "products", schema = "catalog")
public class ProductEntity {

    @Id
    private String id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String thumbnail;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductCategoryEntity> productCategories = new HashSet<>();

    protected ProductEntity() {}

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getThumbnail() { return thumbnail; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}