package com.exemplo.auth.repository;

import com.exemplo.auth.model.MarketListing;
import com.exemplo.auth.model.MarketListing.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MarketListingRepository extends JpaRepository<MarketListing, Long> {

    List<MarketListing> findBySellerIdAndStatus(Long sellerId, Status status);

    List<MarketListing> findBySellerIdAndStatusNot(Long sellerId, Status status);

    @Query("""
        select m from MarketListing m
         where m.status = 'ACTIVE'
           and ( lower(m.pokemonName) like lower(concat('%', :q, '%'))
              or lower(coalesce(m.cardName, '')) like lower(concat('%', :q, '%')) )
         order by m.price asc, m.createdAt desc
    """)
    List<MarketListing> searchActiveByQuery(String q);
}

