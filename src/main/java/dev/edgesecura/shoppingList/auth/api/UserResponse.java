package dev.edgesecura.shoppingList.auth.api;

import dev.edgesecura.shoppingList.auth.entity.UserEntity;

public record UserResponse(
        Long id,
        String email,
        String displayName
) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName()
        );
    }
}
