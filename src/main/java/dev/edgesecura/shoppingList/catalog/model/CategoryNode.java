package dev.edgesecura.shoppingList.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryNode {

    private Long id;
    private String name;
    private List<CategoryNode> categories = new ArrayList<>();
    private List<ProductNode> products = new ArrayList<>();

    public CategoryNode(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CategoryNode> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryNode> categories) {
        this.categories = categories;
    }

    public List<ProductNode> getProducts() {
        return products;
    }

    public void setProducts(List<ProductNode> products) {
        this.products = products;
    }
}