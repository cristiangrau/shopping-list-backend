package dev.edgesecura.shoppingList.userdata;

import dev.edgesecura.shoppingList.auth.jwt.AuthPrincipal;
import dev.edgesecura.shoppingList.userdata.api.BasketRequest;
import dev.edgesecura.shoppingList.userdata.api.BasketResponse;
import dev.edgesecura.shoppingList.userdata.api.ShoppingListRequest;
import dev.edgesecura.shoppingList.userdata.api.ShoppingListResponse;
import dev.edgesecura.shoppingList.userdata.entity.ShoppingListEntity;
import dev.edgesecura.shoppingList.userdata.entity.ShoppingListItemEntity;
import dev.edgesecura.shoppingList.userdata.entity.ShoppingListStatus;
import dev.edgesecura.shoppingList.userdata.repository.ShoppingListItemRepository;
import dev.edgesecura.shoppingList.userdata.repository.ShoppingListRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/me")
public class UserDataController {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ShoppingListRepository listRepo;
    private final ShoppingListItemRepository itemRepo;

    public UserDataController(ShoppingListRepository listRepo, ShoppingListItemRepository itemRepo) {
        this.listRepo = listRepo;
        this.itemRepo = itemRepo;
    }

    // ── Cart (active list) ─────────────────────────────────────

    @GetMapping("/basket")
    @Transactional
    public BasketResponse getBasket(@AuthenticationPrincipal AuthPrincipal principal) {
        Long userId = requireUser(principal);
        ShoppingListEntity active = getOrCreateActiveList(userId);
        List<ShoppingListItemEntity> rows = itemRepo.findByListIdOrderByPositionAsc(active.getId());
        return new BasketResponse(rows.stream().map(this::rowToItemMap).toList(), active.getUpdatedAt());
    }

    @PutMapping("/basket")
    @Transactional
    public BasketResponse putBasket(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody BasketRequest request
    ) {
        Long userId = requireUser(principal);
        ShoppingListEntity active = getOrCreateActiveList(userId);
        itemRepo.deleteByListId(active.getId());
        itemRepo.flush();
        List<ShoppingListItemEntity> created = itemRepo.saveAll(
                buildItemEntities(active.getId(), request.items())
        );
        active.setTotal(computeTotal(request.items()));
        listRepo.save(active);
        return new BasketResponse(created.stream().map(this::rowToItemMap).toList(), OffsetDateTime.now());
    }

    // ── Saved lists ────────────────────────────────────────────

    @GetMapping("/lists")
    public List<ShoppingListResponse> getLists(@AuthenticationPrincipal AuthPrincipal principal) {
        Long userId = requireUser(principal);
        return listRepo.findByUserIdAndStatusOrderByCompletedAtDesc(userId, ShoppingListStatus.COMPLETED).stream()
                .map(list -> toListResponse(
                        list,
                        itemRepo.findByListIdOrderByPositionAsc(list.getId())
                ))
                .toList();
    }

    @PostMapping("/lists")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ShoppingListResponse saveList(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ShoppingListRequest request
    ) {
        Long userId = requireUser(principal);
        UUID listId = request.id() != null ? request.id() : UUID.randomUUID();

        ShoppingListEntity list = new ShoppingListEntity();
        list.setId(listId);
        list.setUserId(userId);
        list.setStatus(ShoppingListStatus.COMPLETED);
        list.setTotal(request.total());
        list.setCompletedAt(request.completedAt() != null ? request.completedAt() : OffsetDateTime.now());
        list = listRepo.save(list);

        List<ShoppingListItemEntity> created = itemRepo.saveAll(
                buildItemEntities(listId, request.items())
        );
        return toListResponse(list, created);
    }

    @DeleteMapping("/lists/{id}")
    @Transactional
    public ResponseEntity<Void> deleteList(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id
    ) {
        Long userId = requireUser(principal);
        return listRepo.findById(id)
                .filter(l -> l.getUserId().equals(userId)
                        && l.getStatus() == ShoppingListStatus.COMPLETED)
                .map(l -> {
                    listRepo.delete(l);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ── Helpers ────────────────────────────────────────────────

    private Long requireUser(AuthPrincipal principal) {
        if (principal == null) throw new AccessDeniedException("Authenticated user required");
        return principal.userId();
    }

    private ShoppingListEntity getOrCreateActiveList(Long userId) {
        return listRepo.findByUserIdAndStatus(userId, ShoppingListStatus.ACTIVE)
                .orElseGet(() -> {
                    ShoppingListEntity list = new ShoppingListEntity();
                    list.setId(UUID.randomUUID());
                    list.setUserId(userId);
                    list.setStatus(ShoppingListStatus.ACTIVE);
                    return listRepo.save(list);
                });
    }

    private List<ShoppingListItemEntity> buildItemEntities(UUID listId, List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        List<ShoppingListItemEntity> out = new ArrayList<>(items.size());
        int position = 0;
        for (Map<String, Object> item : items) {
            Object productObj = item.get("product");
            if (!(productObj instanceof Map<?, ?> rawMap)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> productMap = (Map<String, Object>) rawMap;
            String productId = String.valueOf(productMap.get("id"));
            String productName = firstString(productMap, "displayName", "name", "title");
            int qty = item.get("qty") instanceof Number n ? n.intValue() : 1;

            ShoppingListItemEntity entity = new ShoppingListItemEntity();
            entity.setListId(listId);
            entity.setPosition(position++);
            entity.setProductId(productId);
            entity.setProductName(productName);
            entity.setProductPayload(serialize(productMap));
            entity.setQty(qty);
            out.add(entity);
        }
        return out;
    }

    private Map<String, Object> rowToItemMap(ShoppingListItemEntity row) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", row.getProductId());
        map.put("product", parse(row.getProductPayload()));
        map.put("qty", row.getQty());
        return map;
    }

    private ShoppingListResponse toListResponse(ShoppingListEntity list, List<ShoppingListItemEntity> rows) {
        return new ShoppingListResponse(
                list.getId(),
                rows.stream().map(this::rowToItemMap).toList(),
                list.getTotal(),
                list.getCompletedAt() != null ? list.getCompletedAt() : list.getCreatedAt()
        );
    }

    private static java.math.BigDecimal computeTotal(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) return null;
        java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
        boolean any = false;
        for (Map<String, Object> item : items) {
            if (!(item.get("product") instanceof Map<?, ?> rawMap)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> product = (Map<String, Object>) rawMap;
            java.math.BigDecimal price = extractPrice(product);
            if (price == null) continue;
            int qty = item.get("qty") instanceof Number n ? n.intValue() : 1;
            sum = sum.add(price.multiply(java.math.BigDecimal.valueOf(qty)));
            any = true;
        }
        return any ? sum.setScale(2, java.math.RoundingMode.HALF_UP) : null;
    }

    private static java.math.BigDecimal extractPrice(Map<String, Object> product) {
        for (String key : new String[]{"price", "currentPrice", "unitPrice", "unit_price", "salePrice", "sale_price", "retailPrice"}) {
            Object value = product.get(key);
            java.math.BigDecimal direct = asBigDecimal(value);
            if (direct != null) return direct;
            if (value instanceof Map<?, ?> nested) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) nested;
                for (String inner : new String[]{"amount", "value", "gross", "incVat", "current", "price", "sale"}) {
                    java.math.BigDecimal nv = asBigDecimal(m.get(inner));
                    if (nv != null) return nv;
                }
            }
        }
        Object pi = product.get("price_instructions");
        if (pi instanceof Map<?, ?> nested) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) nested;
            for (String inner : new String[]{"unit_price", "bulk_price", "reference_price", "previous_unit_price"}) {
                java.math.BigDecimal nv = asBigDecimal(m.get(inner));
                if (nv != null) return nv;
            }
        }
        return null;
    }

    private static java.math.BigDecimal asBigDecimal(Object value) {
        if (value instanceof Number n) return java.math.BigDecimal.valueOf(n.doubleValue());
        if (value instanceof String s && !s.isBlank()) {
            try { return new java.math.BigDecimal(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static String firstString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                String s = value.toString();
                if (!s.isBlank()) return s;
            }
        }
        return "";
    }

    private String serialize(Object payload) {
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Invalid JSON payload", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyMap();
        try {
            return MAPPER.readValue(raw, MAP_TYPE);
        } catch (JacksonException e) {
            return Collections.emptyMap();
        }
    }
}
