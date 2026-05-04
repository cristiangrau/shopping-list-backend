package dev.edgesecura.shoppingList.catalog.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.edgesecura.shoppingList.catalog.entity.ProductEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    private String id;
    private String displayName;
    private String thumbnail;
    private String unitPrice;

    public ProductResponse() {}

    public ProductResponse(String id, String displayName, String thumbnail, String unitPrice) {
        this.id = id;
        this.displayName = displayName;
        this.thumbnail = thumbnail;
        this.unitPrice = unitPrice;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public String getUnitPrice() { return unitPrice; }
    public void setUnitPrice(String unitPrice) { this.unitPrice = unitPrice; }

    public ProductResponse toProductResponse(ProductEntity entity) {
        ProductResponse r = new ProductResponse();
        r.setId(entity.getId());
        r.setDisplayName(entity.getDisplayName());
        r.setThumbnail(entity.getThumbnail());
        r.setUnitPrice(entity.getUnitPrice().toPlainString());
        return r;
    }
}