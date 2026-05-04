package dev.edgesecura.shoppingList.catalog.repository;

import dev.edgesecura.shoppingList.catalog.entity.ProductCategoryEntity;
import dev.edgesecura.shoppingList.catalog.entity.ProductCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategoryEntity, ProductCategoryId> {

    List<ProductCategoryEntity> findByProductId(String productId);

    List<ProductCategoryEntity> findByCategoryId(Long categoryId);

    void deleteByProductId(String productId);

    void deleteByCategoryId(Long categoryId);
}