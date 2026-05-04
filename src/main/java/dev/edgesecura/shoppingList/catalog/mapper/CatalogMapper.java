package dev.edgesecura.shoppingList.catalog.mapper;

import dev.edgesecura.shoppingList.catalog.api.CategoryDetailsResponse;
import dev.edgesecura.shoppingList.catalog.api.CategoryResponse;
import dev.edgesecura.shoppingList.catalog.api.ProductResponse;
import dev.edgesecura.shoppingList.catalog.entity.CategoryEntity;
import dev.edgesecura.shoppingList.catalog.entity.ProductCategoryEntity;
import dev.edgesecura.shoppingList.catalog.entity.ProductEntity;
import dev.edgesecura.shoppingList.catalog.model.ProductNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CatalogMapper {

    // ----------------------------
    // CATEGORY (light) - NO products
    // ----------------------------

    public CategoryResponse toCategoryResponse(
            CategoryEntity entity,
            boolean includeChildren,
            boolean sortByName
    ) {
        CategoryResponse response = new CategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());

        if (includeChildren) {
            List<CategoryEntity> childrenEntities = safeChildren(entity);

            if (!childrenEntities.isEmpty()) {
                List<CategoryResponse> children = childrenEntities.stream()
                        .map(child -> toCategoryResponse(child, true, sortByName))
                        .collect(Collectors.toCollection(ArrayList::new));

                if (sortByName) {
                    children.sort(Comparator.comparing(CategoryResponse::getName, String.CASE_INSENSITIVE_ORDER));
                }

                response.setCategories(children);
            }
        }

        return response;
    }

    public CategoryResponse toCategoryResponse(
            CategoryEntity entity,
            boolean includeChildren,
            boolean includeDescendantProducts,
            boolean sortByName
    ) {

        CategoryResponse response = new CategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());

        // ---- CHILDREN ----
        if (includeChildren && entity.getChildren() != null && !entity.getChildren().isEmpty()) {

            List<CategoryResponse> children = entity.getChildren()
                    .stream()
                    .map(child -> toCategoryResponse(
                            child,
                            includeChildren,
                            includeDescendantProducts,
                            sortByName
                    ))
                    .toList();

            response.setCategories(children);
        }

        // ---- PRODUCTS ----
        if (includeDescendantProducts
                && entity.getProductCategories() != null
                && !entity.getProductCategories().isEmpty()) {

            List<ProductResponse> products = entity.getProductCategories()
                    .stream()
                    .map(p -> {
                        ProductResponse pr = new ProductResponse();
                        pr.setId(p.getProduct().getId());
                        pr.setDisplayName(p.getProduct().getDisplayName());
                        pr.setThumbnail(p.getProduct().getThumbnail());
                        pr.setUnitPrice(p.getProduct().getUnitPrice().toPlainString());
                        return pr;
                    })
                    .toList();

            if (sortByName) {
                products = products.stream()
                        .sorted((a, b) ->
                                a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
                        .toList();
            }

            response.setProducts(products);
        }

        return response;
    }

    // ----------------------------
    // CATEGORY (details) - CAN include products
    // ----------------------------

    public CategoryResponse toCategoryResponse(
            CategoryEntity entity,
            boolean includeChildren,
            boolean includeDescendantProducts,
            boolean leafProductsOnly,
            boolean sortByName
    ) {
        CategoryResponse response = new CategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());

        List<CategoryEntity> children = safeChildren(entity);
        boolean isLeaf = children.isEmpty();

        if (includeChildren && !children.isEmpty()) {
            List<CategoryResponse> mappedChildren = children.stream()
                    .map(child -> toCategoryResponse(
                            child,
                            true,
                            includeDescendantProducts,
                            leafProductsOnly,
                            sortByName
                    ))
                    .collect(Collectors.toCollection(ArrayList::new));

            if (sortByName) {
                mappedChildren.sort(Comparator.comparing(CategoryResponse::getName, String.CASE_INSENSITIVE_ORDER));
            }

            response.setCategories(mappedChildren);
        }

        boolean includeProductsHere = includeDescendantProducts && (!leafProductsOnly || isLeaf);

        if (includeProductsHere) {
            List<ProductResponse> products = safeProductsViaJoin(entity).stream()
                    .map(this::toProductResponse)
                    .collect(Collectors.toCollection(ArrayList::new));

            if (sortByName) {
                products.sort(Comparator.comparing(ProductResponse::getDisplayName, String.CASE_INSENSITIVE_ORDER));
            }

            if (!products.isEmpty()) {
                response.setProducts(products);
            }
        }

        return response;
    }

    private CategoryDetailsResponse mapCategoryDetails(
            CategoryEntity entity,
            boolean includeChildren,
            boolean allowProductsHere,
            boolean allowProductsInDescendants,
            boolean leafProductsOnly,
            boolean sortByName
    ) {
        CategoryDetailsResponse response = new CategoryDetailsResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());

        List<CategoryEntity> childrenEntities = safeChildren(entity);
        boolean isLeaf = childrenEntities.isEmpty();

        // children
        if (includeChildren && !childrenEntities.isEmpty()) {
            List<CategoryDetailsResponse> children = childrenEntities.stream()
                    .map(child -> mapCategoryDetails(
                            child,
                            true,
                            allowProductsInDescendants,    // only include products in children if allowed
                            allowProductsInDescendants,
                            leafProductsOnly,
                            sortByName
                    ))
                    .collect(Collectors.toCollection(ArrayList::new));

            if (sortByName) {
                children.sort(Comparator.comparing(CategoryDetailsResponse::getName, String.CASE_INSENSITIVE_ORDER));
            }

            response.setCategories(children);
        }

        // products (only on nodes that are allowed)
        boolean shouldIncludeProductsHere = allowProductsHere && (!leafProductsOnly || isLeaf);
        if (shouldIncludeProductsHere) {
            List<ProductEntity> productsEntities = safeProductsViaJoin(entity);

            if (!productsEntities.isEmpty()) {
                List<ProductResponse> products = productsEntities.stream()
                        .map(this::toProductResponse)
                        .collect(Collectors.toCollection(ArrayList::new));

                if (sortByName) {
                    products.sort(Comparator.comparing(ProductResponse::getDisplayName, String.CASE_INSENSITIVE_ORDER));
                }

                response.setProducts(products);

                if (sortByName) {
                    products.sort(Comparator.comparing(ProductResponse::getDisplayName, String.CASE_INSENSITIVE_ORDER));                }

                response.setProducts(products);
            }
        }

        return response;
    }

    private List<CategoryEntity> safeChildren(CategoryEntity entity) {
        if (entity.getChildren() == null || entity.getChildren().isEmpty()) return List.of();
        return new ArrayList<>(entity.getChildren());
    }

    private List<ProductEntity> safeProductsViaJoin(CategoryEntity entity) {
        if (entity.getProductCategories() == null || entity.getProductCategories().isEmpty()) return List.of();

        return entity.getProductCategories().stream()
                .map(ProductCategoryEntity::getProduct)
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    // ----------------------------
    // PRODUCT
    // ----------------------------

    public ProductNode toProductNode(ProductEntity entity) {
        ProductNode node = new ProductNode();

        // id must be String (your requirement)
        node.setId(entity.getId());

        node.setDisplayName(entity.getDisplayName());
        node.setThumbnail(entity.getThumbnail());
        node.setUnitPrice(entity.getUnitPrice());

        return node;
    }

    public ProductResponse toProductResponse(ProductEntity entity) {
        ProductResponse r = new ProductResponse();
        r.setId(entity.getId());                 // String
        r.setDisplayName(entity.getDisplayName());
        r.setThumbnail(entity.getThumbnail());
        r.setUnitPrice(entity.getUnitPrice().toPlainString());   // BigDecimal or String depending on your DTO
        return r;
    }
}