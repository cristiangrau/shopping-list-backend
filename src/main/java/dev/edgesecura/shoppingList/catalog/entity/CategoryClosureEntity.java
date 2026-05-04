package dev.edgesecura.shoppingList.catalog.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "category_closure", schema = "catalog")
public class CategoryClosureEntity {

    @EmbeddedId
    private CategoryClosureId id;

    @Column(nullable = false)
    private Integer depth;

    protected CategoryClosureEntity() {}

    public CategoryClosureId getId() { return id; }
    public Integer getDepth() { return depth; }

    @Embeddable
    public static class CategoryClosureId {
        @Column(name = "ancestor_id")
        private Long ancestorId;

        @Column(name = "descendant_id")
        private Long descendantId;

        protected CategoryClosureId() {}

        public Long getAncestorId() { return ancestorId; }
        public Long getDescendantId() { return descendantId; }
    }
}