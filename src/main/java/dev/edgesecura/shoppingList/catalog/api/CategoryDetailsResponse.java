package dev.edgesecura.shoppingList.catalog.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryDetailsResponse {

    private long id;
    private String name;
    private List<CategoryDetailsResponse> categories; // children
    private List<ProductResponse> products;           // only where present

    public CategoryDetailsResponse() {}

    public CategoryDetailsResponse(long id, String name,
                                   List<CategoryDetailsResponse> categories,
                                   List<ProductResponse> products) {
        this.id = id;
        this.name = name;
        this.categories = categories;
        this.products = products;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<CategoryDetailsResponse> getCategories() { return categories; }
    public void setCategories(List<CategoryDetailsResponse> categories) { this.categories = categories; }

    public List<ProductResponse> getProducts() { return products; }
    public void setProducts(List<ProductResponse> products) { this.products = products; }
}