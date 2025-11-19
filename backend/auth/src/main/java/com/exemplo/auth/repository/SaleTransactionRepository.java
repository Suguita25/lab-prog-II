package com.exemplo.auth.repository;

import com.exemplo.auth.model.SaleTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SaleTransactionRepository extends JpaRepository<SaleTransaction, Long> {
    
    // Transações do comprador
    List<SaleTransaction> findByBuyerId(Long buyerId);
    
    // Transações do vendedor
    List<SaleTransaction> findBySellerId(Long sellerId);
    
    // Transações por status
    List<SaleTransaction> findByStatus(SaleTransaction.TransactionStatus status);
    
    // Histórico completo de um usuário
    @Query("SELECT t FROM SaleTransaction t WHERE t.buyerId = :userId OR t.sellerId = :userId ORDER BY t.createdAt DESC")
    List<SaleTransaction> findUserTransactionHistory(@Param("userId") Long userId);
    
    // Contar transações completadas de um vendedor
    long countBySellerIdAndStatus(Long sellerId, SaleTransaction.TransactionStatus status);
    
    // Contar transações completadas de um comprador
    long countByBuyerIdAndStatus(Long buyerId, SaleTransaction.TransactionStatus status);
    
    // Buscar transação por oferta
    @Query("SELECT t FROM SaleTransaction t WHERE t.offer.id = :offerId")
    Optional<SaleTransaction> findByOfferId(@Param("offerId") Long offerId);
}
