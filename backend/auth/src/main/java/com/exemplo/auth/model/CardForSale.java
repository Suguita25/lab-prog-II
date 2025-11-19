package com.exemplo.auth.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Card do inventário de um usuário marcado para venda
 */
@Entity
@Table(name = "card_for_sale")
public class CardForSale {
    
    public enum CardCondition {
        MINT,           // Perfeita
        NEAR_MINT,      // Quase perfeita
        EXCELLENT,      // Excelente
        GOOD,           // Boa
        LIGHT_PLAYED,   // Pouco jogada
        PLAYED,         // Jogada
        POOR            // Ruim
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Referência ao CardItem original do dono
    @ManyToOne(optional = false)
    @JoinColumn(name = "card_item_id", nullable = false)
    private CardItem cardItem;
    
    // Dono (vendedor)
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;
    
    // Preço pedido
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal askingPrice;
    
    @Column(nullable = false, length = 3)
    private String currency = "BRL";
    
    // Condição do card
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardCondition cardCondition;
    
    // Quantidade disponível para venda
    @Column(nullable = false)
    private int quantity = 1;
    
    // Notas do vendedor
    @Column(length = 500)
    private String sellerNotes;
    
    // Características especiais
    private boolean isFirstEdition = false;
    private boolean isShadowless = false;
    private boolean isGraded = false;
    
    @Column(length = 20)
    private String gradeCompany;  // PSA, BGS, CGC
    
    private Double gradeValue;    // 10, 9.5, etc.
    
    // Metadados
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public CardItem getCardItem() { return cardItem; }
    public void setCardItem(CardItem cardItem) { this.cardItem = cardItem; }
    
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    
    public BigDecimal getAskingPrice() { return askingPrice; }
    public void setAskingPrice(BigDecimal askingPrice) { this.askingPrice = askingPrice; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public CardCondition getCardCondition() { return cardCondition; }
    public void setCardCondition(CardCondition cardCondition) { this.cardCondition = cardCondition; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public String getSellerNotes() { return sellerNotes; }
    public void setSellerNotes(String sellerNotes) { this.sellerNotes = sellerNotes; }
    
    public boolean isFirstEdition() { return isFirstEdition; }
    public void setFirstEdition(boolean firstEdition) { isFirstEdition = firstEdition; }
    
    public boolean isShadowless() { return isShadowless; }
    public void setShadowless(boolean shadowless) { isShadowless = shadowless; }
    
    public boolean isGraded() { return isGraded; }
    public void setGraded(boolean graded) { isGraded = graded; }
    
    public String getGradeCompany() { return gradeCompany; }
    public void setGradeCompany(String gradeCompany) { this.gradeCompany = gradeCompany; }
    
    public Double getGradeValue() { return gradeValue; }
    public void setGradeValue(Double gradeValue) { this.gradeValue = gradeValue; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
