package dev.edgesecura.shoppingList.catalog.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CategoryResponse {

    private long id;
    private String name;
    private List<CategoryResponse> categories;
    private List<ProductResponse> products;

    public CategoryResponse() {}

    public CategoryResponse(long id, String name, List<CategoryResponse> categories) {
        this.id = id;
        this.name = name;
        this.categories = categories;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<CategoryResponse> getCategories() { return categories; }
    public void setCategories(List<CategoryResponse> categories) { this.categories = categories; }

    public List<ProductResponse> getProducts() {
        return products;
    }

    public void setProducts(List<ProductResponse> products) {
        this.products = products;
    }
}