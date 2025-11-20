package com.exemplo.auth.controller;

import com.exemplo.auth.model.MarketListing;
import com.exemplo.auth.repository.UserRepository;
import com.exemplo.auth.service.MarketService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketService market;
    private final UserRepository users;

    public MarketController(MarketService market, UserRepository users) {
        this.market = market;
        this.users = users;
    }

    /* ===== auth helper ===== */

    private Long currentUserId(HttpSession session) {
        Boolean auth = (Boolean) session.getAttribute("auth");
        if (auth == null || !auth) throw new Unauthorized();
        String email = (String) session.getAttribute("email");
        return users.findByEmail(email).orElseThrow().getId();
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class Unauthorized extends RuntimeException {}

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> handleBad(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /* ===== scanner + criação de anúncio ===== */

    @PostMapping(value = "/listings/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MarketListing> scanAndCreate(
            @RequestParam("price") String priceStr,
            @RequestParam("file") MultipartFile file,
            HttpSession session) throws Exception {

        Long uid = currentUserId(session);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Imagem obrigatória.");
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceStr.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Preço inválido.");
        }

        File tmp = File.createTempFile("market_", "_" + (file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename()));
        try {
            file.transferTo(tmp);
            Path storage = Path.of("data", "users", String.valueOf(uid), "market");
            MarketListing m = market.createListingFromScan(uid, tmp, storage, price);
            return ResponseEntity.status(HttpStatus.CREATED).body(m);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    /* ===== buscas / minha lista ===== */

    @GetMapping("/listings/search")
    public List<MarketListing> search(@RequestParam(name = "q", required = false) String q) {
        return market.searchActive(q);
    }

    @GetMapping("/listings/mine")
    public List<MarketListing> myListings(HttpSession session) {
        Long uid = currentUserId(session);
        return market.myActive(uid);
    }

    @GetMapping("/notifications")
    public List<MarketListing> mySales(HttpSession session) {
        Long uid = currentUserId(session);
        return market.mySold(uid); // usado como "notificação de venda"
    }

    @PatchMapping("/listings/{id}")
    public ResponseEntity<MarketListing> updateListing(@PathVariable Long id,
                                                    @RequestBody Map<String,String> body,
                                                    HttpSession session) {
        Long uid = currentUserId(session);
        String newName = body.get("pokemonName");
        String newPrice = body.get("price");
        MarketListing m = market.updateListing(uid, id, newName, newPrice);
        return ResponseEntity.ok(m);
    }


    /* ===== comprar / cancelar ===== */

    @PostMapping("/listings/{id}/buy")
    public ResponseEntity<MarketListing> buy(@PathVariable Long id, HttpSession session) {
        Long uid = currentUserId(session);
        MarketListing m = market.buy(uid, id);
        return ResponseEntity.ok(m);
    }

    @DeleteMapping("/listings/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id, HttpSession session) {
        Long uid = currentUserId(session);
        market.cancel(uid, id);
        return ResponseEntity.noContent().build();
    }
}
