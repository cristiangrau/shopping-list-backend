package dev.edgesecura.shoppingList.catalog.repository;

import dev.edgesecura.shoppingList.catalog.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    @Query("select c from CategoryEntity c where c.parent is null")
    List<CategoryEntity> findRoots();

    Page<CategoryEntity> findByParentIsNull(Pageable pageable);
}