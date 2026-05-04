package dev.edgesecura.shoppingList.catalog.api;

import org.springframework.data.domain.Page;

import java.util.List;

public final class Paging {

    private Paging() {}

    public static <T> PagedResponse<T> of(List<T> items, long total, int offset, int limit) {
        return new PagedResponse<T>(items, total, offset, limit);
    }

    public static <T> PagedResponse<T> of(Page<T> page, int offset, int limit) {
        return new PagedResponse<T>(page.getContent(), page.getTotalElements(), offset, limit);
    }
}