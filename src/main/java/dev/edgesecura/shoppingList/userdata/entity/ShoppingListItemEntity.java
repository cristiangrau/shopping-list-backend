package dev.edgesecura.shoppingList.userdata.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "shopping_list_items", schema = "catalog")
public class ShoppingListItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "list_id", nullable = false)
    private UUID listId;

    @Column(nullable = false)
    private int position;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_payload", nullable = false, columnDefinition = "jsonb")
    private String productPayload;

    @Column(nullable = false)
    private int qty;

    public Long getId() { return id; }
    public UUID getListId() { return listId; }
    public int getPosition() { return position; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductPayload() { return productPayload; }
    public int getQty() { return qty; }

    public void setListId(UUID listId) { this.listId = listId; }
    public void setPosition(int position) { this.position = position; }
    public void setProductId(String productId) { this.productId = productId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setProductPayload(String productPayload) { this.productPayload = productPayload; }
    public void setQty(int qty) { this.qty = qty; }
}
