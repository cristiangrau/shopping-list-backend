package dev.edgesecura.shoppingList.auth.api;

public record AuthResponse(
        UserResponse user,
        TokenPair tokens
) {
}
