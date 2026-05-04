package dev.edgesecura.shoppingList.catalog;

import dev.edgesecura.shoppingList.catalog.api.CategoryResponse;
import dev.edgesecura.shoppingList.catalog.api.PagedResponse;
import dev.edgesecura.shoppingList.catalog.api.Paging;
import dev.edgesecura.shoppingList.catalog.api.ProductResponse;
import dev.edgesecura.shoppingList.catalog.api.SortDir;
import dev.edgesecura.shoppingList.catalog.entity.CategoryEntity;
import dev.edgesecura.shoppingList.catalog.entity.ProductEntity;
import dev.edgesecura.shoppingList.catalog.mapper.CatalogMapper;
import dev.edgesecura.shoppingList.catalog.model.CategoryNode;
import dev.edgesecura.shoppingList.catalog.model.ProductNode;
import dev.edgesecura.shoppingList.catalog.repository.CategoryClosureRepository;
import dev.edgesecura.shoppingList.catalog.repository.CategoryRepository;
import dev.edgesecura.shoppingList.catalog.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final CategoryClosureRepository closureRepository;
    private final ProductRepository productRepository;
    private final CatalogMapper mapper;

    public CatalogService(
            CategoryRepository categoryRepository,
            CategoryClosureRepository closureRepository,
            ProductRepository productRepository,
            CatalogMapper mapper
    ) {
        this.categoryRepository = categoryRepository;
        this.closureRepository = closureRepository;
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<CategoryResponse> listCategories(
            int offset,
            int limit,
            String sortBy,
            SortDir dir,
            boolean rootOnly,
            boolean includeChildren,
            boolean includeDescendantProducts,
            boolean leafProductsOnly,
            boolean sortByName
    ) {
        Sort.Direction direction = dir == SortDir.desc ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                offset / limit,
                limit,
                Sort.by(direction, sortBy)
        );

        Page<CategoryEntity> page = rootOnly
                ? categoryRepository.findByParentIsNull(pageable)
                : categoryRepository.findAll(pageable);

        List<CategoryResponse> items = page.getContent()
                .stream()
                .map(c -> mapper.toCategoryResponse(
                        c,
                        includeChildren,
                        includeDescendantProducts,
                        leafProductsOnly,
                        sortByName
                ))
                .toList();

        return new PagedResponse<>(
                items,
                page.getTotalElements(),
                offset,
                limit
        );
    }

    @Transactional(readOnly = true)
    public CategoryNode getCategory(
            long id,
            boolean includeChildren,
            boolean includeDescendantProducts,
            boolean leafProductsOnly,
            boolean sortByName
    ) {
        CategoryEntity root = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category not found: " + id));

        CategoryNode rootNode = new CategoryNode(root.getId(), root.getName());

        if (!includeChildren && !includeDescendantProducts) {
            rootNode.setCategories(List.of());
            rootNode.setProducts(List.of());
            return rootNode;
        }

        // All descendants (including self)
        List<Long> descendantIds = closureRepository.findDescendantIds(id);
        Set<Long> descendantIdSet = new HashSet<>(descendantIds);

        // Load all categories in that subtree and build a tree
        List<CategoryEntity> subtreeCategories = categoryRepository.findAllById(descendantIds);

        List<CategoryNode> treeAsList = buildTreeFromAllCategories(
                subtreeCategories,
                descendantIdSet,
                sortByName,
                includeDescendantProducts,
                includeChildren
        );

        // treeAsList contains the node(s) that are roots in the provided set.
        // Since we queried a single subtree, it should be exactly one root node.
        CategoryNode builtRoot = treeAsList.stream()
                .filter(n -> Objects.equals(n.getId(), id))
                .findFirst()
                .orElse(rootNode);

        if (includeDescendantProducts) {
            attachProductsToTree(builtRoot, descendantIdSet, leafProductsOnly);
        } else {
            // ensure products empty arrays
            clearProductsRecursively(builtRoot);
        }

        if (!includeChildren) {
            // If includeChildren=false, strip categories but keep products behavior depending on flags:
            builtRoot.setCategories(List.of());
        }

        return builtRoot;
    }

    /**
     * Flat list of products under category subtree (handy endpoint)
     */
    @Transactional(readOnly = true)
    public List<ProductNode> listProductsInSubtree(long categoryId) {
        List<Long> descendantIds = closureRepository.findDescendantIds(categoryId);
        List<ProductEntity> products = productRepository.findProductsByCategoryIds(descendantIds);

        return products.stream()
                .map(this::toProductNode)
                .collect(Collectors.toList());
    }

    // ----------------- helpers -----------------

    private List<CategoryNode> buildTreeFromAllCategories(
            List<CategoryEntity> categories,
            Set<Long> allowedIdsOrNull,
            boolean sortByName,
            boolean prepareProductsLists,
            boolean includeChildren
    ) {
        Map<Long, CategoryNode> byId = new HashMap<>();

        for (CategoryEntity c : categories) {
            if (allowedIdsOrNull != null && !allowedIdsOrNull.isEmpty() && !allowedIdsOrNull.contains(c.getId())) {
                continue;
            }
            CategoryNode node = new CategoryNode(c.getId(), c.getName());
            node.setProducts(List.of()); // always non-null
            node.setCategories(new ArrayList<>());
            byId.put(c.getId(), node);
        }

        // Link parent->child
        for (CategoryEntity c : categories) {
            CategoryNode node = byId.get(c.getId());
            if (node == null) continue;

            CategoryEntity parent = c.getParent();
            if (parent != null && byId.containsKey(parent.getId())) {
                byId.get(parent.getId()).getCategories().add(node);
            }
        }

        // Roots are nodes whose parent is null OR parent not in our set
        List<CategoryNode> roots = new ArrayList<>();
        for (CategoryEntity c : categories) {
            CategoryNode node = byId.get(c.getId());
            if (node == null) continue;

            CategoryEntity parent = c.getParent();
            if (parent == null || !byId.containsKey(parent.getId())) {
                roots.add(node);
            }
        }

        if (sortByName) {
            sortTreeRecursively(roots);
        }

        if (!includeChildren) {
            roots.forEach(r -> r.setCategories(List.of()));
        }

        if (!prepareProductsLists) {
            clearProductsRecursivelyList(roots);
        }

        return roots;
    }

    private void attachProductsToTree(CategoryNode root, Set<Long> subtreeIds, boolean leafOnly) {
        // Load products + their category_id in one shot for this subtree
        List<Object[]> rows = productRepository.findProductsWithCategoryId(subtreeIds);

        Map<Long, List<ProductNode>> productsByCategory = new HashMap<>();
        for (Object[] row : rows) {
            // row: category_id, then product columns in order of products table
            Long categoryId = ((Number) row[0]).longValue();

            String productId = (String) row[1];
            String displayName = (String) row[2];
            String thumbnail = (String) row[3];
            Object unitPriceObj = row[4];

            BigDecimal unitPrice = null;

            if (unitPriceObj != null) {
                if (unitPriceObj instanceof BigDecimal) {
                    unitPrice = (BigDecimal) unitPriceObj;
                } else if (unitPriceObj instanceof Number) {
                    unitPrice = BigDecimal.valueOf(((Number) unitPriceObj).doubleValue());
                } else {
                    unitPrice = new BigDecimal(unitPriceObj.toString());
                }
            }

            productsByCategory
                    .computeIfAbsent(categoryId, k -> new ArrayList<>())
                    .add(new ProductNode(productId, displayName, thumbnail, unitPrice));
        }

        // Attach to nodes
        attachProductsRecursively(root, productsByCategory, leafOnly);
    }

    private void attachProductsRecursively(CategoryNode node, Map<Long, List<ProductNode>> productsByCategory, boolean leafOnly) {
        boolean isLeaf = node.getCategories() == null || node.getCategories().isEmpty();

        List<ProductNode> products = productsByCategory.getOrDefault(node.getId(), List.of());

        if (!leafOnly || isLeaf) {
            // products already sorted by query
            node.setProducts(products);
        } else {
            node.setProducts(List.of());
        }

        if (node.getCategories() != null) {
            for (CategoryNode child : node.getCategories()) {
                attachProductsRecursively(child, productsByCategory, leafOnly);
            }
        }
    }

    private void sortTreeRecursively(List<CategoryNode> nodes) {
        nodes.sort(Comparator.comparing(CategoryNode::getName, String.CASE_INSENSITIVE_ORDER));
        for (CategoryNode n : nodes) {
            if (n.getCategories() != null && !n.getCategories().isEmpty()) {
                sortTreeRecursively(n.getCategories());
            }
        }
    }

    private void clearProductsRecursively(CategoryNode node) {
        node.setProducts(List.of());
        if (node.getCategories() != null) {
            for (CategoryNode child : node.getCategories()) {
                clearProductsRecursively(child);
            }
        }
    }

    private void clearProductsRecursivelyList(List<CategoryNode> nodes) {
        for (CategoryNode n : nodes) {
            clearProductsRecursively(n);
        }
    }

    private CategoryResponse toCategoryResponse(CategoryEntity entity, boolean includeChildren) {

        CategoryResponse response = new CategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());

        if (includeChildren) {
            List<CategoryResponse> children = entity.getChildren().stream()
                    .map(child -> toCategoryResponse(child, includeChildren))
                    .toList();

            if (!children.isEmpty()) {
                response.setCategories(children);
            }
        }

        return response;
    }

    private ProductNode toProductNode(ProductEntity p) {
        BigDecimal unitPrice = p.getUnitPrice() == null ? null : p.getUnitPrice();
        return new ProductNode(p.getId(), p.getDisplayName(), p.getThumbnail(), unitPrice);
    }

    public PagedResponse<ProductNode> getProducts(int offset, int limit, String sortBy, SortDir dir) {
        Sort sort = (dir == SortDir.desc)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        int pageNumber = offset / limit;

        Page<ProductEntity> page = productRepository.findAll(PageRequest.of(pageNumber, limit, sort));
        Page<ProductNode> mapped = page.map(this::toProductNode);

        return Paging.of(mapped, offset, limit);
    }

    public ProductNode getProductById(String id) {
        return productRepository.findById(id)
                .map(this::toProductNode)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProductsByCategoryId(
            long categoryId,
            int offset,
            int limit,
            String sortBy,
            SortDir dir
    ) {
        int pageNumber = offset / limit;

        Sort sort = Sort.by(dir == SortDir.asc ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(pageNumber, limit, sort);

        Page<ProductEntity> page = productRepository.findByCategoryId(categoryId, pageable);

        List<ProductResponse> items = page.getContent()
                .stream()
                .map(mapper::toProductResponse)
                .toList();

        return new PagedResponse<>(
                items,
                page.getTotalElements(),
                offset,
                limit
        );
    }

    public List<ProductNode> searchProducts(String q, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100)); // prevent crazy limits
        Page<ProductEntity> page = productRepository.findByDisplayNameContainingIgnoreCase(
                q,
                PageRequest.of(0, safeLimit, Sort.by("displayName").ascending())
        );

        return page.getContent().stream()
                .map(this::toProductNode)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryTree(
            long id,
            boolean includeChildren,
            boolean includeDescendantProducts,
            boolean sortByName
    ) {

        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));

        return mapper.toCategoryResponse(
                category,
                includeChildren,
                includeDescendantProducts,
                sortByName
        );
    }
}