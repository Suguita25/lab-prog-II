package com.exemplo.auth.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "market_listings")
public class MarketListing {

    public enum Status { ACTIVE, SOLD, CANCELED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sellerId;

    @Column
    private Long buyerId; // null enquanto não vender

    @Column(nullable = false, length = 120)
    private String pokemonName;

    @Column(length = 160)
    private String cardName;

    @Column(length = 255)
    private String imagePath; // URL tipo /files/users/{id}/market/xxx.png

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant soldAt;

    /* lifecycle */
    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    /* getters/setters */

    public Long getId() { return id; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }

    public String getPokemonName() { return pokemonName; }
    public void setPokemonName(String pokemonName) { this.pokemonName = pokemonName; }

    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getSoldAt() { return soldAt; }
    public void setSoldAt(Instant soldAt) { this.soldAt = soldAt; }
}

