package com.exemplo.auth.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transação de venda completada
 */
@Entity
@Table(name = "sale_transaction")
public class SaleTransaction {
    
    public enum TransactionStatus {
        PENDING_PAYMENT,    // Aguardando pagamento
        PAID,              // Pago, aguardando envio
        SHIPPED,           // Enviado
        DELIVERED,         // Entregue
        COMPLETED,         // Transação completa
        CANCELLED,         // Cancelada
        DISPUTED,          // Em disputa
        REFUNDED           // Reembolsada
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Referência à proposta aceita
    @OneToOne(optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private SaleOffer offer;
    
    // Vendedor e Comprador
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;
    
    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;
    
    // Card vendido (nome do Pokémon)
    @Column(name = "card_name", nullable = false, length = 120)
    private String cardName;
    
    // Valores
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Column(nullable = false)
    private int quantity;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING_PAYMENT;
    
    // Informações de envio
    @Column(length = 100)
    private String trackingNumber;
    
    @Column(length = 50)
    private String shippingCarrier;
    
    @Column(name = "shipped_at")
    private Instant shippedAt;
    
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    
    // Notas
    @Column(name = "buyer_notes", length = 500)
    private String buyerNotes;
    
    @Column(name = "seller_notes", length = 500)
    private String sellerNotes;
    
    // Datas
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "paid_at")
    private Instant paidAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public SaleOffer getOffer() { return offer; }
    public void setOffer(SaleOffer offer) { this.offer = offer; }
    
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    
    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    
    public String getShippingCarrier() { return shippingCarrier; }
    public void setShippingCarrier(String shippingCarrier) { this.shippingCarrier = shippingCarrier; }
    
    public Instant getShippedAt() { return shippedAt; }
    public void setShippedAt(Instant shippedAt) { this.shippedAt = shippedAt; }
    
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    
    public String getBuyerNotes() { return buyerNotes; }
    public void setBuyerNotes(String buyerNotes) { this.buyerNotes = buyerNotes; }
    
    public String getSellerNotes() { return sellerNotes; }
    public void setSellerNotes(String sellerNotes) { this.sellerNotes = sellerNotes; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
}
