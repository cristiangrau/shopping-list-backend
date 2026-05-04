package dev.edgesecura.shoppingList.auth;

import dev.edgesecura.shoppingList.auth.api.AuthResponse;
import dev.edgesecura.shoppingList.auth.api.LoginRequest;
import dev.edgesecura.shoppingList.auth.api.RefreshRequest;
import dev.edgesecura.shoppingList.auth.api.RegisterRequest;
import dev.edgesecura.shoppingList.auth.api.TokenPair;
import dev.edgesecura.shoppingList.auth.api.UserResponse;
import dev.edgesecura.shoppingList.auth.entity.UserEntity;
import dev.edgesecura.shoppingList.auth.jwt.AuthPrincipal;
import dev.edgesecura.shoppingList.auth.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(request.email(), request.password());
        return new AuthResponse(UserResponse.from(result.user()), result.tokens());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request.email(), request.password());
        return new AuthResponse(UserResponse.from(result.user()), result.tokens());
    }

    @PostMapping("/refresh")
    public TokenPair refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest body) {
        authService.logout(body == null ? null : body.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Not signed in");
        UserEntity user = users.findById(principal.userId())
                .orElseThrow(() -> new IllegalStateException("User missing"));
        return UserResponse.from(user);
    }
}
