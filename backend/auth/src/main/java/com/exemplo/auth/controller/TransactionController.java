package com.exemplo.auth.controller;

import com.exemplo.auth.model.SaleTransaction;
import com.exemplo.auth.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    
    private final TransactionService service;
    
    public TransactionController(TransactionService service) {
        this.service = service;
    }
    
    private Long getUserId(HttpSession session) {
        Boolean auth = (Boolean) session.getAttribute("auth");
        if (auth == null || !auth) throw new Unauthorized();
        Object userId = session.getAttribute("userId");
        if (userId == null) throw new Unauthorized();
        return ((Number) userId).longValue();
    }
    
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class Unauthorized extends RuntimeException {}
    
    @GetMapping("/history")
    public ResponseEntity<List<SaleTransaction>> getUserHistory(HttpSession session) {
        Long userId = getUserId(session);
        return ResponseEntity.ok(service.getUserHistory(userId));
    }
    
    @GetMapping("/purchases")
    public ResponseEntity<List<SaleTransaction>> getUserPurchases(HttpSession session) {
        Long userId = getUserId(session);
        return ResponseEntity.ok(service.getUserPurchases(userId));
    }
    
    @GetMapping("/sales")
    public ResponseEntity<List<SaleTransaction>> getUserSales(HttpSession session) {
        Long userId = getUserId(session);
        return ResponseEntity.ok(service.getUserSales(userId));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SaleTransaction> getTransactionDetails(@PathVariable Long id, HttpSession session) {
        Long userId = getUserId(session);
        return ResponseEntity.ok(service.getTransactionDetails(userId, id));
    }
    
    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<SaleTransaction> markAsPaid(@PathVariable Long id, HttpSession session) {
        Long userId = getUserId(session);
        return ResponseEntity.ok(service.markAsPaid(userId, id));
    }
    
    @PostMapping("/{id}/shipping")
    public ResponseEntity<SaleTransaction> addShippingInfo(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body,
                                                          HttpSession session) {
        Long userId = getUserId(session);
        String trackingNumber = body.get("trackingNumber");
        String carrier = body.get("carrier");
        return ResponseEntity.ok(service.addShippingInfo(userId, id, trackingNumber, carrier));
    }
    
    @PostMapping("/{id}/confirm-delivery")
    public ResponseEntity<SaleTransaction> confirmDelivery(@PathVariable Long id, HttpSession session) {
        Long userId = getUserId(session);
        return ResponseEntity.ok(service.confirmDelivery(userId, id));
    }
    
    @PostMapping("/{id}/complete")
    public ResponseEntity<SaleTransaction> completeTransaction(@PathVariable Long id, HttpSession session) {
        Long userId = getUserId(session);
        return ResponseEntity.ok(service.completeTransaction(userId, id));
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<SaleTransaction> cancelTransaction(@PathVariable Long id,
                                                            @RequestBody(required = false) Map<String, String> body,
                                                            HttpSession session) {
        Long userId = getUserId(session);
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(service.cancelTransaction(userId, id, reason));
    }
    
    @PostMapping("/{id}/dispute")
    public ResponseEntity<SaleTransaction> openDispute(@PathVariable Long id, HttpSession session) {
        Long userId = getUserId(session);
        return ResponseEntity.ok(service.openDispute(userId, id));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}