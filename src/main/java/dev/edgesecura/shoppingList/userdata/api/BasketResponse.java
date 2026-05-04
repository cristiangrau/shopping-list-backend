package dev.edgesecura.shoppingList.userdata.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record BasketResponse(
        List<Map<String, Object>> items,
        OffsetDateTime updatedAt
) {
}
