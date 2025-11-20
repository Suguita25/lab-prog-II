package com.exemplo.auth.service;

import com.exemplo.auth.model.MarketListing;
import com.exemplo.auth.model.MarketListing.Status;
import com.exemplo.auth.repository.MarketListingRepository;
import com.exemplo.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Service
public class MarketService {

    private final MarketListingRepository listings;
    private final UserRepository users;
    private final OcrService ocr;
    private final PokemonDictionary dict;

    public MarketService(MarketListingRepository listings,
                         UserRepository users,
                         OcrService ocr,
                         PokemonDictionary dict) {
        this.listings = listings;
        this.users = users;
        this.ocr = ocr;
        this.dict = dict;
    }

    /* ===== criar anúncio a partir do scanner ===== */

    @Transactional
    public MarketListing createListingFromScan(Long sellerId,
                                               File tempImage,
                                               Path storageBase,
                                               BigDecimal price) throws Exception {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Preço deve ser maior que zero.");
        }

        // 1) OCR parecido com scanAndAdd
        String candidate = null;
        try {
            String ocrName = ocr.extractCardName(tempImage);
            if (ocrName != null && !ocrName.isBlank()) {
                candidate = ocrName.trim();
            }
        } catch (Exception ignore) {}

        if (candidate == null || candidate.isBlank()) {
            try {
                String raw = ocr.extractText(tempImage);
                if (raw != null) {
                    candidate = raw.trim();
                }
            } catch (Exception ignore) {}
        }

        String pokemonName = dict.bestMatchLoose(candidate != null ? candidate : "")
                .orElse("Unknown");

        // 2) salva imagem em data/users/{id}/market
        Files.createDirectories(storageBase);
        String baseName = tempImage.getName();
        String ext = baseName.contains(".")
                ? baseName.substring(baseName.lastIndexOf('.'))
                : ".png";
        String safeName = "market_" + System.currentTimeMillis()
                + "_" + Math.abs(baseName.hashCode()) + ext;
        Path target = storageBase.resolve(safeName);
        Files.move(tempImage.toPath(), target);

        String webPath = "/files/users/" + sellerId + "/market/" + safeName;

        // 3) cria listing
        MarketListing m = new MarketListing();
        m.setSellerId(sellerId);
        m.setPokemonName(pokemonName);
        m.setCardName(candidate != null && !candidate.isBlank() ? candidate : pokemonName);
        m.setImagePath(webPath);
        m.setPrice(price);
        m.setStatus(Status.ACTIVE);
        m.setCreatedAt(Instant.now());

        return listings.save(m);
    }

    /* ===== buscas / listagens ===== */

    @Transactional(readOnly = true)
    public List<MarketListing> searchActive(String query) {
        String q = (query == null || query.isBlank()) ? "" : query.trim();
        if (q.isEmpty()) {
            return listings.searchActiveByQuery(""); // retorna tudo
        }
        return listings.searchActiveByQuery(q);
    }

    @Transactional(readOnly = true)
    public List<MarketListing> myActive(Long sellerId) {
        return listings.findBySellerIdAndStatus(sellerId, Status.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<MarketListing> mySold(Long sellerId) {
        return listings.findBySellerIdAndStatus(sellerId, Status.SOLD);
    }

    @Transactional
    public MarketListing updateListing(Long sellerId,
                                    Long listingId,
                                    String newPokemonName,
                                    String newPriceStr) {
        MarketListing m = listings.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Anúncio não encontrado"));

        if (!sellerId.equals(m.getSellerId())) {
            throw new IllegalArgumentException("Você não pode editar anúncio de outro usuário.");
        }
        if (m.getStatus() != MarketListing.Status.ACTIVE) {
            throw new IllegalArgumentException("Só é possível editar anúncios ativos.");
        }

        boolean changed = false;

        if (newPokemonName != null) {
            String trimmed = newPokemonName.trim();
            if (!trimmed.isEmpty()) {
                m.setPokemonName(trimmed);
                changed = true;
            }
        }

        if (newPriceStr != null) {
            String norm = newPriceStr.trim().replace(",", ".");
            if (!norm.isEmpty()) {
                try {
                    java.math.BigDecimal price = new java.math.BigDecimal(norm);
                    if (price.signum() <= 0) {
                        throw new IllegalArgumentException("Preço deve ser maior que zero.");
                    }
                    m.setPrice(price);
                    changed = true;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Preço inválido.");
                }
            }
        }

        if (!changed) return m; // nada pra atualizar

        return listings.save(m);
    }


    /* ===== comprar / remover ===== */

    @Transactional
    public MarketListing buy(Long buyerId, Long listingId) {
        MarketListing m = listings.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Anúncio não encontrado"));

        if (m.getStatus() != Status.ACTIVE) {
            throw new IllegalArgumentException("Este anúncio não está mais disponível.");
        }
        if (buyerId.equals(m.getSellerId())) {
            throw new IllegalArgumentException("Você não pode comprar sua própria carta.");
        }

        // garante que comprador existe
        users.findById(buyerId).orElseThrow();

        m.setBuyerId(buyerId);
        m.setStatus(Status.SOLD);
        m.setSoldAt(Instant.now());
        return listings.save(m);
    }

    @Transactional
    public void cancel(Long sellerId, Long listingId) {
        MarketListing m = listings.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Anúncio não encontrado"));
        if (!sellerId.equals(m.getSellerId())) {
            throw new IllegalArgumentException("Você não pode cancelar anúncio de outro usuário.");
        }
        if (m.getStatus() != Status.ACTIVE) {
            throw new IllegalArgumentException("Anúncio já não está ativo.");
        }
        m.setStatus(Status.CANCELED);
        listings.save(m);
    }
}

