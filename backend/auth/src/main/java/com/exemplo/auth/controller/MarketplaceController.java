package com.exemplo.auth.controller;

import com.exemplo.auth.model.*;
import com.exemplo.auth.repository.UserRepository;
import com.exemplo.auth.service.MarketplaceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {
    
    private final MarketplaceService service;
    private final UserRepository userRepo;
    
    public MarketplaceController(MarketplaceService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }
    
    /**
     * Obtém o userId da sessão. Se não houver, tenta buscar pelo email.
     */
    private Long getUserId(HttpSession session) {
        Boolean auth = (Boolean) session.getAttribute("auth");
        if (auth == null || !auth) {
            throw new UnauthorizedException("Não autenticado");
        }
        
        // Tenta pegar direto da sessão
        Object userId = session.getAttribute("userId");
        if (userId != null) {
            if (userId instanceof Long) return (Long) userId;
            if (userId instanceof Number) return ((Number) userId).longValue();
        }
        
        // Fallback: busca pelo email
        String email = (String) session.getAttribute("email");
        if (email != null) {
            return userRepo.findByEmail(email)
                    .map(User::getId)
                    .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));
        }
        
        throw new UnauthorizedException("Sessão inválida. Faça login novamente.");
    }
    
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String msg) { super(msg); }
    }
    
    // ===== CARDS À VENDA =====
    
    @PostMapping("/mark-for-sale")
    public ResponseEntity<?> markForSale(@RequestBody Map<String, Object> body, HttpSession session) {
        try {
            Long userId = getUserId(session);
            Long cardItemId = ((Number) body.get("cardItemId")).longValue();
            BigDecimal price = new BigDecimal(body.get("price").toString());
            String currency = body.getOrDefault("currency", "BRL").toString();
            CardForSale.CardCondition condition = CardForSale.CardCondition.valueOf(
                    body.get("condition").toString()
            );
            int quantity = body.containsKey("quantity") 
                    ? ((Number) body.get("quantity")).intValue() 
                    : 1;
            String notes = (String) body.get("notes");
            
            CardForSale sale = service.markForSale(userId, cardItemId, price, currency, condition, quantity, notes);
            return ResponseEntity.ok(sale);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/remove-from-sale/{saleId}")
    public ResponseEntity<?> removeFromSale(@PathVariable Long saleId, HttpSession session) {
        try {
            Long userId = getUserId(session);
            service.removeFromSale(userId, saleId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PatchMapping("/update-sale/{saleId}")
    public ResponseEntity<?> updateSale(@PathVariable Long saleId,
                                        @RequestBody Map<String, Object> body,
                                        HttpSession session) {
        try {
            Long userId = getUserId(session);
            BigDecimal newPrice = body.containsKey("price") 
                    ? new BigDecimal(body.get("price").toString()) 
                    : null;
            String newNotes = (String) body.get("notes");
            
            CardForSale sale = service.updateSale(userId, saleId, newPrice, newNotes);
            return ResponseEntity.ok(sale);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
    public ResponseEntity<?> getMyCardsForSale(HttpSession session) {
        try {
            Long userId = getUserId(session);
            return ResponseEntity.ok(service.getMyCardsForSale(userId));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ===== PROPOSTAS =====
    
    @PostMapping("/make-offer")
    public ResponseEntity<?> makeOffer(@RequestBody Map<String, Object> body, HttpSession session) {
        try {
            Long buyerId = getUserId(session);
            Long cardForSaleId = ((Number) body.get("cardForSaleId")).longValue();
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            int quantity = body.containsKey("quantity") 
                    ? ((Number) body.get("quantity")).intValue() 
                    : 1;
            String message = (String) body.get("message");
            
            SaleOffer offer = service.makeOffer(buyerId, cardForSaleId, amount, quantity, message);
            return ResponseEntity.status(HttpStatus.CREATED).body(offer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/received-offers")
    public ResponseEntity<?> getReceivedOffers(@RequestParam(required = false) String status,
                                               HttpSession session) {
        try {
            Long sellerId = getUserId(session);
            SaleOffer.OfferStatus offerStatus = (status != null && !status.isEmpty()) 
                    ? SaleOffer.OfferStatus.valueOf(status) 
                    : null;
            return ResponseEntity.ok(service.getReceivedOffers(sellerId, offerStatus));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/made-offers")
    public ResponseEntity<?> getMadeOffers(@RequestParam(required = false) String status,
                                           HttpSession session) {
        try {
            Long buyerId = getUserId(session);
            SaleOffer.OfferStatus offerStatus = (status != null && !status.isEmpty()) 
                    ? SaleOffer.OfferStatus.valueOf(status) 
                    : null;
            return ResponseEntity.ok(service.getMadeOffers(buyerId, offerStatus));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/accept-offer/{offerId}")
    public ResponseEntity<?> acceptOffer(@PathVariable Long offerId, HttpSession session) {
        try {
            Long sellerId = getUserId(session);
            SaleTransaction transaction = service.acceptOffer(sellerId, offerId);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/reject-offer/{offerId}")
    public ResponseEntity<?> rejectOffer(@PathVariable Long offerId,
                                         @RequestBody(required = false) Map<String, String> body,
                                         HttpSession session) {
        try {
            Long sellerId = getUserId(session);
            String reason = (body != null) ? body.get("reason") : null;
            service.rejectOffer(sellerId, offerId, reason);
            return ResponseEntity.ok(Map.of("status", "rejected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/cancel-offer/{offerId}")
    public ResponseEntity<?> cancelOffer(@PathVariable Long offerId, HttpSession session) {
        try {
            Long buyerId = getUserId(session);
            service.cancelOffer(buyerId, offerId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}