package dev.edgesecura.shoppingList.catalog.model;

import java.math.BigDecimal;

public class ProductNode {
    private String id;
    private String displayName;
    private String thumbnail;
    private BigDecimal unitPrice;

    public ProductNode() {}

    public ProductNode(String id, String displayName, String thumbnail, BigDecimal unitPrice) {
        this.id = id;
        this.displayName = displayName;
        this.thumbnail = thumbnail;
        this.unitPrice = unitPrice;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}