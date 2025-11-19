package com.exemplo.auth.controller;

import com.exemplo.auth.model.*;
import com.exemplo.auth.service.MarketplaceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.exemplo.auth.repository.UserRepository;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {
    //private final UserRepository users;
    
    private final MarketplaceService service;
    
    public MarketplaceController(MarketplaceService service) {
        this.service = service;
    }
    
    private Long getUserId(HttpSession session) {
        Boolean auth = (Boolean) session.getAttribute("auth");
        if (auth == null || !auth) throw new Unauthorized();
        // Você precisa ajustar isso para obter o ID do usuário da sessão
        // Por exemplo, armazenar na sessão durante o login
        Object userId = session.getAttribute("userId");
        if (userId == null) throw new Unauthorized();
        return ((Number) userId).longValue();
    }
    
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class Unauthorized extends RuntimeException {}
    
    // ===== CARDS À VENDA =====
    
    @PostMapping("/mark-for-sale")
    public ResponseEntity<CardForSale> markForSale(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = getUserId(session);
        Long cardItemId = ((Number) body.get("cardItemId")).longValue();
        BigDecimal price = new BigDecimal(body.get("price").toString());
        String currency = body.getOrDefault("currency", "BRL").toString();
        CardForSale.CardCondition condition = CardForSale.CardCondition.valueOf(body.get("condition").toString());
        int quantity = ((Number) body.getOrDefault("quantity", 1)).intValue();
        String notes = (String) body.get("notes");
        
        CardForSale sale = service.markForSale(userId, cardItemId, price, currency, condition, quantity, notes);
        return ResponseEntity.ok(sale);
    }
    
    @DeleteMapping("/remove-from-sale/{saleId}")
    public ResponseEntity<Void> removeFromSale(@PathVariable Long saleId, HttpSession session) {
        Long userId = getUserId(session);
        service.removeFromSale(userId, saleId);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/update-sale/{saleId}")
    public ResponseEntity<CardForSale> updateSale(@PathVariable Long saleId,
                                                  @RequestBody Map<String, Object> body,
                                                  HttpSession session) {
        Long userId = getUserId(session);
        BigDecimal newPrice = body.containsKey("price") ? new BigDecimal(body.get("price").toString()) : null;
        String newNotes = (String) body.get("notes");
        
        CardForSale sale = service.updateSale(userId, saleId, newPrice, newNotes);
        return ResponseEntity.ok(sale);
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<CardForSale>> getAvailableCards() {
        return ResponseEntity.ok(service.getAvailableCards());
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<CardForSale>> searchByPokemon(@RequestParam String name) {
        return ResponseEntity.ok(service.searchByPokemon(name));
    }
    
    @GetMapping("/my-sales")
    public ResponseEntity<List<CardForSale>> getMyCardsForSale(HttpSession session) {
        Long userId = getUserId(session);
        return ResponseEntity.ok(service.getMyCardsForSale(userId));
    }
    
    // ===== PROPOSTAS =====
    
    @PostMapping("/make-offer")
    public ResponseEntity<SaleOffer> makeOffer(@RequestBody Map<String, Object> body, HttpSession session) {
        Long buyerId = getUserId(session);
        Long cardForSaleId = ((Number) body.get("cardForSaleId")).longValue();
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        int quantity = ((Number) body.getOrDefault("quantity", 1)).intValue();
        String message = (String) body.get("message");
        
        SaleOffer offer = service.makeOffer(buyerId, cardForSaleId, amount, quantity, message);
        return ResponseEntity.status(HttpStatus.CREATED).body(offer);
    }
    
    @GetMapping("/received-offers")
    public ResponseEntity<List<SaleOffer>> getReceivedOffers(@RequestParam(required = false) String status,
                                                             HttpSession session) {
        Long sellerId = getUserId(session);
        SaleOffer.OfferStatus offerStatus = status != null ? SaleOffer.OfferStatus.valueOf(status) : null;
        return ResponseEntity.ok(service.getReceivedOffers(sellerId, offerStatus));
    }
    
    @GetMapping("/made-offers")
    public ResponseEntity<List<SaleOffer>> getMadeOffers(@RequestParam(required = false) String status,
                                                         HttpSession session) {
        Long buyerId = getUserId(session);
        SaleOffer.OfferStatus offerStatus = status != null ? SaleOffer.OfferStatus.valueOf(status) : null;
        return ResponseEntity.ok(service.getMadeOffers(buyerId, offerStatus));
    }
    
    @PostMapping("/accept-offer/{offerId}")
    public ResponseEntity<SaleTransaction> acceptOffer(@PathVariable Long offerId, HttpSession session) {
        Long sellerId = getUserId(session);
        SaleTransaction transaction = service.acceptOffer(sellerId, offerId);
        return ResponseEntity.ok(transaction);
    }
    
    @PostMapping("/reject-offer/{offerId}")
    public ResponseEntity<Void> rejectOffer(@PathVariable Long offerId,
                                           @RequestBody Map<String, String> body,
                                           HttpSession session) {
        Long sellerId = getUserId(session);
        String reason = body.get("reason");
        service.rejectOffer(sellerId, offerId, reason);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/cancel-offer/{offerId}")
    public ResponseEntity<Void> cancelOffer(@PathVariable Long offerId, HttpSession session) {
        Long buyerId = getUserId(session);
        service.cancelOffer(buyerId, offerId);
        return ResponseEntity.noContent().build();
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
} 
    

