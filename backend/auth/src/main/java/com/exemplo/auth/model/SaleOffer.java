
package com.exemplo.auth.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Proposta de compra feita por um usuário
 */
@Entity
@Table(name = "sale_offer")
public class SaleOffer {
    
    public enum OfferStatus {
        PENDING,      // Aguardando resposta do vendedor
        ACCEPTED,     // Aceita pelo vendedor
        REJECTED,     // Rejeitada pelo vendedor
        CANCELLED,    // Cancelada pelo comprador
        COMPLETED,    // Venda completada
        EXPIRED       // Expirada
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Card à venda
    @ManyToOne(optional = false)
    @JoinColumn(name = "card_for_sale_id", nullable = false)
    private CardForSale cardForSale;
    
    // Comprador (quem fez a proposta)
    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;
    
    // Vendedor (dono do card)
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;
    
    // Valor oferecido
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal offerAmount;
    
    @Column(nullable = false, length = 3)
    private String currency = "BRL";
    
    // Quantidade desejada
    @Column(nullable = false)
    private int quantity = 1;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status = OfferStatus.PENDING;
    
    // Mensagem do comprador
    @Column(length = 500)
    private String buyerMessage;
    
    // Resposta do vendedor
    @Column(length = 500)
    private String sellerResponse;
    
    // Datas
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "responded_at")
    private Instant respondedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public CardForSale getCardForSale() { return cardForSale; }
    public void setCardForSale(CardForSale cardForSale) { this.cardForSale = cardForSale; }
    
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    
    public BigDecimal getOfferAmount() { return offerAmount; }
    public void setOfferAmount(BigDecimal offerAmount) { this.offerAmount = offerAmount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public OfferStatus getStatus() { return status; }
    public void setStatus(OfferStatus status) { this.status = status; }
    
    public String getBuyerMessage() { return buyerMessage; }
    public void setBuyerMessage(String buyerMessage) { this.buyerMessage = buyerMessage; }
    
    public String getSellerResponse() { return sellerResponse; }
    public void setSellerResponse(String sellerResponse) { this.sellerResponse = sellerResponse; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
    
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
