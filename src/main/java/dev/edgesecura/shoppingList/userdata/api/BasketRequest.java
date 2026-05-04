package dev.edgesecura.shoppingList.userdata.api;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record BasketRequest(
        @NotNull List<Map<String, Object>> items
) {
}
