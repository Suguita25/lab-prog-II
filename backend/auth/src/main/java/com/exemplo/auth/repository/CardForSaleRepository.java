package com.exemplo.auth.repository;

import com.exemplo.auth.model.CardForSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CardForSaleRepository extends JpaRepository<CardForSale, Long> {
    
    // Buscar cards à venda de um vendedor específico
    List<CardForSale> findBySellerId(Long sellerId);
    
    // Buscar todos os cards disponíveis para venda (marketplace público)
    @Query("SELECT c FROM CardForSale c WHERE c.quantity > 0 ORDER BY c.createdAt DESC")
    List<CardForSale> findAllAvailable();
    
    // Buscar por nome do Pokémon (aproximado)
    @Query("SELECT c FROM CardForSale c WHERE c.quantity > 0 AND LOWER(c.cardItem.pokemonName) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY c.askingPrice ASC")
    List<CardForSale> findByPokemonName(@Param("name") String name);
    
    // Buscar por condição
    @Query("SELECT c FROM CardForSale c WHERE c.quantity > 0 AND c.cardCondition = :condition ORDER BY c.askingPrice ASC")
    List<CardForSale> findByCondition(@Param("condition") CardForSale.CardCondition condition);
    
    // Verificar se um CardItem já está à venda
    @Query("SELECT c FROM CardForSale c WHERE c.cardItem.id = :cardItemId AND c.quantity > 0")
    Optional<CardForSale> findByCardItemId(@Param("cardItemId") Long cardItemId);
    
    // Contar cards à venda de um vendedor
    long countBySellerId(Long sellerId);
}