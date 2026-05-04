package dev.edgesecura.shoppingList.catalog.repository;

import dev.edgesecura.shoppingList.catalog.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, String> {

    @Query(value = """
        SELECT p.*
        FROM catalog.products p
        JOIN catalog.product_category pc ON pc.product_id = p.id
        WHERE pc.category_id IN (:categoryIds)
        ORDER BY lower(p.display_name) ASC
        """, nativeQuery = true)
    List<ProductEntity> findProductsByCategoryIds(@Param("categoryIds") Collection<Long> categoryIds);

    @Query(value = """
        SELECT pc.category_id
        FROM catalog.product_category pc
        WHERE pc.category_id IN (:categoryIds)
        """, nativeQuery = true)
    List<Long> findCategoryIdsThatHaveProducts(@Param("categoryIds") Collection<Long> categoryIds);

    @Query(value = """
        SELECT pc.category_id, p.*
        FROM catalog.products p
        JOIN catalog.product_category pc ON pc.product_id = p.id
        WHERE pc.category_id IN (:categoryIds)
        ORDER BY pc.category_id ASC, lower(p.display_name) ASC
        """, nativeQuery = true)
    List<Object[]> findProductsWithCategoryId(@Param("categoryIds") Collection<Long> categoryIds);

    @Query("""
        select p
        from ProductEntity p
        join ProductCategoryEntity pc on pc.product.id = p.id
        where pc.category.id = :categoryId
        """)
    Page<ProductEntity> findByCategoryId(@Param("categoryId") long categoryId, Pageable pageable);

    Page<ProductEntity> findByDisplayNameContainingIgnoreCase(String q, Pageable pageable);

}