package com.exemplo.auth.dto;


public class CardDto {
    private Long id;
    private String pokemonName;
    private String cardName;
    private String source;
    private String imagePath;  // <-- nome igual ao do front

    // getters e setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPokemonName() { return pokemonName; }
    public void setPokemonName(String pokemonName) { this.pokemonName = pokemonName; }

    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}

