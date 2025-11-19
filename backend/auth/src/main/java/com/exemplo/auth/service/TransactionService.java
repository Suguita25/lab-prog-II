package com.exemplo.auth.service;

import com.exemplo.auth.model.SaleTransaction;
import com.exemplo.auth.repository.SaleTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class TransactionService {
    
    private final SaleTransactionRepository transactionRepo;
    
    public TransactionService(SaleTransactionRepository transactionRepo) {
        this.transactionRepo = transactionRepo;
    }
    
    public List<SaleTransaction> getUserHistory(Long userId) {
        return transactionRepo.findUserTransactionHistory(userId);
    }
    
    public List<SaleTransaction> getUserPurchases(Long userId) {
        return transactionRepo.findByBuyerId(userId);
    }
    
    public List<SaleTransaction> getUserSales(Long userId) {
        return transactionRepo.findBySellerId(userId);
    }
    
    @Transactional
    public SaleTransaction markAsPaid(Long buyerId, Long transactionId) {
        SaleTransaction transaction = transactionRepo.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));
        
        if (!transaction.getBuyerId().equals(buyerId)) {
            throw new IllegalArgumentException("Você não tem permissão");
        }
        
        if (transaction.getStatus() != SaleTransaction.TransactionStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Esta transação não pode ser marcada como paga");
        }
        
        transaction.setStatus(SaleTransaction.TransactionStatus.PAID);
        transaction.setPaidAt(Instant.now());
        
        return transactionRepo.save(transaction);
    }
    
    @Transactional
    public SaleTransaction addShippingInfo(Long sellerId, Long transactionId, 
                                          String trackingNumber, String carrier) {
        SaleTransaction transaction = transactionRepo.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));
        
        if (!transaction.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Você não tem permissão");
        }
        
        transaction.setTrackingNumber(trackingNumber);
        transaction.setShippingCarrier(carrier);
        transaction.setStatus(SaleTransaction.TransactionStatus.SHIPPED);
        transaction.setShippedAt(Instant.now());
        
        return transactionRepo.save(transaction);
    }
    
    @Transactional
    public SaleTransaction confirmDelivery(Long buyerId, Long transactionId) {
        SaleTransaction transaction = transactionRepo.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));
        
        if (!transaction.getBuyerId().equals(buyerId)) {
            throw new IllegalArgumentException("Você não tem permissão");
        }
        
        if (transaction.getStatus() != SaleTransaction.TransactionStatus.SHIPPED) {
            throw new IllegalArgumentException("Esta transação não pode ser confirmada");
        }
        
        transaction.setStatus(SaleTransaction.TransactionStatus.DELIVERED);
        transaction.setDeliveredAt(Instant.now());
        
        return transactionRepo.save(transaction);
    }
    
    @Transactional
    public SaleTransaction completeTransaction(Long buyerId, Long transactionId) {
        SaleTransaction transaction = transactionRepo.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));
        
        if (!transaction.getBuyerId().equals(buyerId)) {
            throw new IllegalArgumentException("Você não tem permissão");
        }
        
        if (transaction.getStatus() != SaleTransaction.TransactionStatus.DELIVERED) {
            throw new IllegalArgumentException("Esta transação não pode ser completada");
        }
        
        transaction.setStatus(SaleTransaction.TransactionStatus.COMPLETED);
        transaction.setCompletedAt(Instant.now());
        
        return transactionRepo.save(transaction);
    }
    
    @Transactional
    public SaleTransaction cancelTransaction(Long userId, Long transactionId, String reason) {
        SaleTransaction transaction = transactionRepo.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));
        
        boolean isSeller = transaction.getSellerId().equals(userId);
        boolean isBuyer = transaction.getBuyerId().equals(userId);
        
        if (!isSeller && !isBuyer) {
            throw new IllegalArgumentException("Você não tem permissão");
        }
        
        if (transaction.getStatus() == SaleTransaction.TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("Transações completadas não podem ser canceladas");
        }
        
        transaction.setStatus(SaleTransaction.TransactionStatus.CANCELLED);
        transaction.setCancelledAt(Instant.now());
        
        if (isSeller) {
            transaction.setSellerNotes(reason);
        } else {
            transaction.setBuyerNotes(reason);
        }
        
        return transactionRepo.save(transaction);
    }
    
    @Transactional
    public SaleTransaction openDispute(Long userId, Long transactionId) {
        SaleTransaction transaction = transactionRepo.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));
        
        boolean isSeller = transaction.getSellerId().equals(userId);
        boolean isBuyer = transaction.getBuyerId().equals(userId);
        
        if (!isSeller && !isBuyer) {
            throw new IllegalArgumentException("Você não tem permissão");
        }
        
        if (transaction.getStatus() == SaleTransaction.TransactionStatus.COMPLETED ||
            transaction.getStatus() == SaleTransaction.TransactionStatus.CANCELLED) {
            throw new IllegalArgumentException("Não é possível abrir disputa");
        }
        
        transaction.setStatus(SaleTransaction.TransactionStatus.DISPUTED);
        
        return transactionRepo.save(transaction);
    }
    
    public SaleTransaction getTransactionDetails(Long userId, Long transactionId) {
        SaleTransaction transaction = transactionRepo.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));
        
        boolean isSeller = transaction.getSellerId().equals(userId);
        boolean isBuyer = transaction.getBuyerId().equals(userId);
        
        if (!isSeller && !isBuyer) {
            throw new IllegalArgumentException("Você não tem permissão");
        }
        
        return transaction;
    }
}
    

