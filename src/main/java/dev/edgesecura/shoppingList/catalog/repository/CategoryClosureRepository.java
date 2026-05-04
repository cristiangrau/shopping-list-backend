package dev.edgesecura.shoppingList.catalog.repository;

import dev.edgesecura.shoppingList.catalog.entity.CategoryClosureEntity;
import dev.edgesecura.shoppingList.catalog.entity.CategoryClosureEntity.CategoryClosureId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryClosureRepository extends JpaRepository<CategoryClosureEntity, CategoryClosureId> {

    @Query(value = """
        SELECT c.*
        FROM catalog.categories c
        JOIN catalog.category_closure cc ON cc.descendant_id = c.id
        WHERE cc.ancestor_id = :ancestorId
        ORDER BY cc.depth ASC, lower(c.name) ASC
        """, nativeQuery = true)
    List<Object[]> findDescendantsRaw(@Param("ancestorId") long ancestorId);

    @Query(value = """
        SELECT descendant_id
        FROM catalog.category_closure
        WHERE ancestor_id = :ancestorId
        """, nativeQuery = true)
    List<Long> findDescendantIds(@Param("ancestorId") long ancestorId);
}