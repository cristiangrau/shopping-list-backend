package dev.edgesecura.shoppingList.catalog.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({ "offset", "limit", "total", "items" })
public class PagedResponse<T> {

    private int offset;
    private int limit;
    private long total;
    private List<T> items;

    public PagedResponse() {}

    public PagedResponse(List<T> items, long total, int offset, int limit) {
        this.items = items;
        this.total = total;
        this.offset = offset;
        this.limit = limit;
    }

    public List<T> getItems() { return items; }
    public int getOffset() { return offset; }
    public int getLimit() { return limit; }
    public long getTotal() { return total; }

    public void setItems(List<T> items) { this.items = items; }
    public void setOffset(int offset) { this.offset = offset; }
    public void setLimit(int limit) { this.limit = limit; }
    public void setTotal(long total) { this.total = total; }
}