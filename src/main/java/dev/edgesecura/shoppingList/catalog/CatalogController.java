package dev.edgesecura.shoppingList.catalog;

import dev.edgesecura.shoppingList.catalog.api.CategoryResponse;
import dev.edgesecura.shoppingList.catalog.api.PagedResponse;
import dev.edgesecura.shoppingList.catalog.api.ProductResponse;
import dev.edgesecura.shoppingList.catalog.api.SortDir;
import dev.edgesecura.shoppingList.catalog.model.CategoryNode;
import dev.edgesecura.shoppingList.catalog.model.ProductNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping({"/categories", "/categories/"})
    public PagedResponse<CategoryResponse> listCategories(
            @RequestParam(defaultValue = "0") @Min(0) @Max(100) int offset,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") SortDir dir,
            @RequestParam(defaultValue = "true") boolean rootOnly,
            @RequestParam(defaultValue = "false") boolean includeChildren,
            @RequestParam(defaultValue = "false") boolean includeDescendantProducts,
            @RequestParam(defaultValue = "true") boolean leafProductsOnly,
            @RequestParam(defaultValue = "true") boolean sortByName
    ) {
        return catalogService.listCategories(
                offset,
                limit,
                sortBy,
                dir,
                rootOnly,
                includeChildren,
                includeDescendantProducts,
                leafProductsOnly,
                sortByName
        );
    }

    @GetMapping({"/categories/{id}", "/categories/{id}/"})
    public CategoryResponse getCategory(
            @PathVariable long id,
            @RequestParam(defaultValue = "true") boolean includeChildren,
            @RequestParam(defaultValue = "true") boolean includeDescendantProducts,
            @RequestParam(defaultValue = "true") boolean sortByName
    ) {
        return catalogService.getCategoryTree(
                id,
                includeChildren,
                includeDescendantProducts,
                sortByName
        );
    }

    @GetMapping("/categories/{id}/products")
    public PagedResponse<ProductResponse> getCategoryProducts(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100) int offset,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "displayName") String sortBy,
            @RequestParam(defaultValue = "asc") SortDir dir
    ) {
        return catalogService.getProductsByCategoryId(id, offset, limit, sortBy, dir);
    }

    @GetMapping({"/products", "/products/"})
    public PagedResponse<ProductNode> getProducts(
            @RequestParam(defaultValue = "0") @Min(0) @Max(100) int offset,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "displayName") String sortBy,
            @RequestParam(defaultValue = "asc") SortDir dir
    ) {
        return catalogService.getProducts(offset, limit, sortBy, dir);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductNode> getProductById(@PathVariable String id) {
        ProductNode product = catalogService.getProductById(id);
        return product == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(product);
    }

    @GetMapping("/products/search")
    public List<ProductNode> searchProducts(
            @RequestParam("q") String q,
            @RequestParam(value = "limit", defaultValue = "25") int limit
    ) {
        return catalogService.searchProducts(q, limit);
    }
}