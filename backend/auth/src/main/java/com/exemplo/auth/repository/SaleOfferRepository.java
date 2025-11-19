package com.exemplo.auth.repository;

import com.exemplo.auth.model.SaleOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SaleOfferRepository extends JpaRepository<SaleOffer, Long> {
    
    // Propostas recebidas (como vendedor)
    List<SaleOffer> findBySellerId(Long sellerId);
    
    // Propostas feitas (como comprador)
    List<SaleOffer> findByBuyerId(Long buyerId);
    
    // Propostas por status
    List<SaleOffer> findBySellerIdAndStatus(Long sellerId, SaleOffer.OfferStatus status);
    
    List<SaleOffer> findByBuyerIdAndStatus(Long buyerId, SaleOffer.OfferStatus status);
    
    // Propostas para um card específico à venda
    @Query("SELECT o FROM SaleOffer o WHERE o.cardForSale.id = :cardForSaleId ORDER BY o.createdAt DESC")
    List<SaleOffer> findByCardForSaleId(@Param("cardForSaleId") Long cardForSaleId);
    
    // Contar propostas pendentes de um vendedor
    long countBySellerIdAndStatus(Long sellerId, SaleOffer.OfferStatus status);
    
    // Contar propostas pendentes de um comprador
    long countByBuyerIdAndStatus(Long buyerId, SaleOffer.OfferStatus status);
}

