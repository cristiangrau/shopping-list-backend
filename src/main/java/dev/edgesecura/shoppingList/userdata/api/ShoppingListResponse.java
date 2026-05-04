package dev.edgesecura.shoppingList.userdata.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ShoppingListResponse(
        UUID id,
        List<Map<String, Object>> items,
        BigDecimal total,
        OffsetDateTime completedAt
) {
}
