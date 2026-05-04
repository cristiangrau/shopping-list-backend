package dev.edgesecura.shoppingList.catalog.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories", schema = "catalog")
public class CategoryEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CategoryEntity parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private Set<CategoryEntity> children = new HashSet<>();

    // Join table relation (product_categories)
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductCategoryEntity> productCategories = new HashSet<>();

    public Long getId() { return id; }
    public String getName() { return name; }

    public CategoryEntity getParent() { return parent; }
    public Set<CategoryEntity> getChildren() { return children; }

    public Set<ProductCategoryEntity> getProductCategories() { return productCategories; }
}