package dev.edgesecura.shoppingList.userdata.api;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ShoppingListRequest(
        UUID id,
        @NotNull List<Map<String, Object>> items,
        BigDecimal total,
        OffsetDateTime completedAt
) {
}
