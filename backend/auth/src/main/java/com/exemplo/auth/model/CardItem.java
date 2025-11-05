package com.exemplo.auth.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(name="card_items")
public class CardItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long folderId;

    @Column(nullable=false)
    private Long userId;

    @Column(nullable=false, length=120)
    private String cardName;

    @Column(length=120)
    private String pokemonName;

    @Column(length=16)
    private String source;       // "manual" | "ocr"

    @Column(length=255)
    private String imagePath;

    @Column(nullable=false)
    private Instant createdAt;

    // getters/setters
    public Long getId() { return id; }
    public Long getFolderId() { return folderId; }
    public Long getUserId() { return userId; }
    public String getCardName() { return cardName; }
    public String getPokemonName() { return pokemonName; }
    public String getSource() { return source; }
    public String getImagePath() { return imagePath; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setCardName(String cardName) { this.cardName = cardName; }
    public void setPokemonName(String pokemonName) { this.pokemonName = pokemonName; }
    public void setSource(String source) { this.source = source; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
     public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
