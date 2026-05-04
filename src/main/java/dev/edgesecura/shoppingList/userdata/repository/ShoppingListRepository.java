package dev.edgesecura.shoppingList.userdata.repository;

import dev.edgesecura.shoppingList.userdata.entity.ShoppingListEntity;
import dev.edgesecura.shoppingList.userdata.entity.ShoppingListStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingListRepository extends JpaRepository<ShoppingListEntity, UUID> {
    Optional<ShoppingListEntity> findByUserIdAndStatus(Long userId, ShoppingListStatus status);
    List<ShoppingListEntity> findByUserIdAndStatusOrderByCompletedAtDesc(Long userId, ShoppingListStatus status);
    long deleteByIdAndUserId(UUID id, Long userId);
}
