package dev.edgesecura.shoppingList.auth.api;

public record TokenPair(
        String accessToken,
        long accessTtlSeconds,
        String refreshToken,
        long refreshTtlSeconds
) {
}
