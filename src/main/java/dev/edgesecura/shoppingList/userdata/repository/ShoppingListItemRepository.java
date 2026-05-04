package dev.edgesecura.shoppingList.userdata.repository;

import dev.edgesecura.shoppingList.userdata.entity.ShoppingListItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItemEntity, Long> {
    List<ShoppingListItemEntity> findByListIdOrderByPositionAsc(UUID listId);

    @Modifying
    @Query("delete from ShoppingListItemEntity i where i.listId = :listId")
    int deleteByListId(@Param("listId") UUID listId);
}
