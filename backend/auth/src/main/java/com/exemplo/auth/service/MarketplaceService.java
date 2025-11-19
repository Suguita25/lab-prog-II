package com.exemplo.auth.service;

import com.exemplo.auth.model.*;
import com.exemplo.auth.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class MarketplaceService {
    
    private final CardForSaleRepository cardForSaleRepo;
    private final SaleOfferRepository offerRepo;
    private final SaleTransactionRepository transactionRepo;
    private final CardItemRepository cardItemRepo;
    private final UserRepository userRepo;
    
    public MarketplaceService(CardForSaleRepository cardForSaleRepo,
                             SaleOfferRepository offerRepo,
                             SaleTransactionRepository transactionRepo,
                             CardItemRepository cardItemRepo,
                             UserRepository userRepo) {
        this.cardForSaleRepo = cardForSaleRepo;
        this.offerRepo = offerRepo;
        this.transactionRepo = transactionRepo;
        this.cardItemRepo = cardItemRepo;
        this.userRepo = userRepo;
    }
    
    // ===== CARDS À VENDA =====
    
    @Transactional
    public CardForSale markForSale(Long userId, Long cardItemId, BigDecimal price, 
                                   String currency, CardForSale.CardCondition condition,
                                   int quantity, String notes) {
        CardItem cardItem = cardItemRepo.findById(cardItemId)
            .orElseThrow(() -> new IllegalArgumentException("Card não encontrado"));
        
        if (!cardItem.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Este card não pertence a você");
        }
        
        // Verifica se já está à venda
        cardForSaleRepo.findByCardItemId(cardItemId).ifPresent(existing -> {
            throw new IllegalArgumentException("Este card já está à venda");
        });
        
        CardForSale sale = new CardForSale();
        sale.setCardItem(cardItem);
        sale.setSellerId(userId);
        sale.setAskingPrice(price);
        sale.setCurrency(currency);
        sale.setCardCondition(condition);
        sale.setQuantity(quantity);
        sale.setSellerNotes(notes);
        sale.setCreatedAt(Instant.now());
        sale.setUpdatedAt(Instant.now());
        
        return cardForSaleRepo.save(sale);
    }
    
    @Transactional
    public void removeFromSale(Long userId, Long saleId) {
        CardForSale sale = cardForSaleRepo.findById(saleId)
            .orElseThrow(() -> new IllegalArgumentException("Anúncio não encontrado"));
        
        if (!sale.getSellerId().equals(userId)) {
            throw new IllegalArgumentException("Você não tem permissão");
        }
        
        // Verifica se há propostas pendentes
        List<SaleOffer> pending = offerRepo.findByCardForSaleId(saleId);
        if (pending.stream().anyMatch(o -> o.getStatus() == SaleOffer.OfferStatus.PENDING)) {
            throw new IllegalArgumentException("Há propostas pendentes para este card");
        }
        
        cardForSaleRepo.delete(sale);
    }
    
    @Transactional
    public CardForSale updateSale(Long userId, Long saleId, BigDecimal newPrice, String newNotes) {
        CardForSale sale = cardForSaleRepo.findById(saleId)
            .orElseThrow(() -> new IllegalArgumentException("Anúncio não encontrado"));
        
        if (!sale.getSellerId().equals(userId)) {
            throw new IllegalArgumentException("Você não tem permissão");
        }
        
        if (newPrice != null) sale.setAskingPrice(newPrice);
        if (newNotes != null) sale.setSellerNotes(newNotes);
        sale.setUpdatedAt(Instant.now());
        
        return cardForSaleRepo.save(sale);
    }
    
    public List<CardForSale> getAvailableCards() {
        return cardForSaleRepo.findAllAvailable();
    }
    
    public List<CardForSale> searchByPokemon(String name) {
        return cardForSaleRepo.findByPokemonName(name);
    }
    
    public List<CardForSale> getMyCardsForSale(Long userId) {
        return cardForSaleRepo.findBySellerId(userId);
    }
    
    // ===== PROPOSTAS =====
    
    @Transactional
    public SaleOffer makeOffer(Long buyerId, Long cardForSaleId, BigDecimal amount, 
                              int quantity, String message) {
        CardForSale sale = cardForSaleRepo.findById(cardForSaleId)
            .orElseThrow(() -> new IllegalArgumentException("Card não encontrado"));
        
        if (sale.getSellerId().equals(buyerId)) {
            throw new IllegalArgumentException("Você não pode fazer proposta para seu próprio card");
        }
        
        if (quantity > sale.getQuantity()) {
            throw new IllegalArgumentException("Quantidade solicitada maior que disponível");
        }
        
        SaleOffer offer = new SaleOffer();
        offer.setCardForSale(sale);
        offer.setBuyerId(buyerId);
        offer.setSellerId(sale.getSellerId());
        offer.setOfferAmount(amount);
        offer.setCurrency(sale.getCurrency());
        offer.setQuantity(quantity);
        offer.setBuyerMessage(message);
        offer.setStatus(SaleOffer.OfferStatus.PENDING);
        offer.setCreatedAt(Instant.now());
        offer.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        
        return offerRepo.save(offer);
    }
    
    public List<SaleOffer> getReceivedOffers(Long sellerId, SaleOffer.OfferStatus status) {
        if (status != null) {
            return offerRepo.findBySellerIdAndStatus(sellerId, status);
        }
        return offerRepo.findBySellerId(sellerId);
    }
    
    public List<SaleOffer> getMadeOffers(Long buyerId, SaleOffer.OfferStatus status) {
        if (status != null) {
            return offerRepo.findByBuyerIdAndStatus(buyerId, status);
        }
        return offerRepo.findByBuyerId(buyerId);
    }
    
    @Transactional
    public SaleTransaction acceptOffer(Long sellerId, Long offerId) {
        SaleOffer offer = offerRepo.findById(offerId)
            .orElseThrow(() -> new IllegalArgumentException("Proposta não encontrada"));
        
        if (!offer.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Esta proposta não é sua");
        }
        
        if (offer.getStatus() != SaleOffer.OfferStatus.PENDING) {
            throw new IllegalArgumentException("Esta proposta não está mais disponível");
        }
        
        // Atualizar proposta
        offer.setStatus(SaleOffer.OfferStatus.ACCEPTED);
        offer.setRespondedAt(Instant.now());
        offerRepo.save(offer);
        
        // Criar transação
        CardForSale sale = offer.getCardForSale();
        SaleTransaction transaction = new SaleTransaction();
        transaction.setOffer(offer);
        transaction.setSellerId(sellerId);
        transaction.setBuyerId(offer.getBuyerId());
        transaction.setCardName(sale.getCardItem().getPokemonName());
        transaction.setAmount(offer.getOfferAmount());
        transaction.setCurrency(offer.getCurrency());
        transaction.setQuantity(offer.getQuantity());
        transaction.setStatus(SaleTransaction.TransactionStatus.PENDING_PAYMENT);
        transaction.setCreatedAt(Instant.now());
        
        // Atualizar quantidade disponível
        sale.setQuantity(sale.getQuantity() - offer.getQuantity());
        if (sale.getQuantity() == 0) {
            cardForSaleRepo.delete(sale);
        } else {
            cardForSaleRepo.save(sale);
        }
        
        return transactionRepo.save(transaction);
    }
    
    @Transactional
    public void rejectOffer(Long sellerId, Long offerId, String reason) {
        SaleOffer offer = offerRepo.findById(offerId)
            .orElseThrow(() -> new IllegalArgumentException("Proposta não encontrada"));
        
        if (!offer.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Esta proposta não é sua");
        }
        
        if (offer.getStatus() != SaleOffer.OfferStatus.PENDING) {
            throw new IllegalArgumentException("Esta proposta não pode ser rejeitada");
        }
        
        offer.setStatus(SaleOffer.OfferStatus.REJECTED);
        offer.setSellerResponse(reason);
        offer.setRespondedAt(Instant.now());
        offerRepo.save(offer);
    }
    
    @Transactional
    public void cancelOffer(Long buyerId, Long offerId) {
        SaleOffer offer = offerRepo.findById(offerId)
            .orElseThrow(() -> new IllegalArgumentException("Proposta não encontrada"));
        
        if (!offer.getBuyerId().equals(buyerId)) {
            throw new IllegalArgumentException("Esta proposta não é sua");
        }
        
        if (offer.getStatus() != SaleOffer.OfferStatus.PENDING) {
            throw new IllegalArgumentException("Esta proposta não pode ser cancelada");
        }
        
        offer.setStatus(SaleOffer.OfferStatus.CANCELLED);
        offerRepo.save(offer);
    }
}
