package com.exemplo.auth.controller;

import com.exemplo.auth.dto.AddCardManualRequest;
import com.exemplo.auth.dto.CreateFolderRequest;
import com.exemplo.auth.model.CardItem;
import com.exemplo.auth.model.CollectionFolder;
import com.exemplo.auth.repository.CardItemRepository;
import com.exemplo.auth.repository.CollectionFolderRepository;
import com.exemplo.auth.repository.UserRepository;
import com.exemplo.auth.service.CollectionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionService service;
    private final UserRepository userRepo;
    private final CollectionFolderRepository folderRepo;
    private final CardItemRepository itemRepo;

    public CollectionController(CollectionService service,
                                UserRepository userRepo,
                                CollectionFolderRepository folderRepo,
                                CardItemRepository itemRepo) {
        this.service = service;
        this.userRepo = userRepo;
        this.folderRepo = folderRepo;
        this.itemRepo = itemRepo;
    }

    // -------- helpers --------
    private Long currentUserId(HttpSession session) {
        if (session == null) throw new Unauthorized();
        Object auth = session.getAttribute("auth");
        Object email = session.getAttribute("email");
        if (!(auth instanceof Boolean) || !(Boolean) auth || email == null) throw new Unauthorized();
        return userRepo.findByEmail(email.toString())
                .map(u -> u.getId())
                .orElseThrow(Unauthorized::new);
    }

    @ResponseStatus(code = org.springframework.http.HttpStatus.UNAUTHORIZED)
    private static class Unauthorized extends RuntimeException {}

    // Converte caminhos antigos ("data\\users\\...") para URL /files/...
    private String normalizeImagePath(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;
        if (imagePath.startsWith("/files/")) return imagePath;

        Path dataRoot = Paths.get("data").toAbsolutePath().normalize();
        Path absoluteTarget = Paths.get(imagePath).toAbsolutePath().normalize();

        try {
            Path relative = dataRoot.relativize(absoluteTarget);
            String rel = relative.toString().replace(File.separatorChar, '/');
            return "/files/" + rel;
        } catch (IllegalArgumentException e) {
            String abs = absoluteTarget.toString().replace(File.separatorChar, '/');
            return "/files/" + abs;
        }
    }

    // -------- endpoints --------

    @PostMapping("/folders")
    public ResponseEntity<?> createFolder(@RequestBody @Valid CreateFolderRequest req, HttpSession session) {
        Long uid = currentUserId(session);
        try {
            CollectionFolder f = service.createFolder(uid, req.name());
            return ResponseEntity.ok(Map.of("id", f.getId(), "name", f.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/folders")
    public List<CollectionFolder> listFolders(HttpSession session) {
        return service.listFolders(currentUserId(session));
    }

    /** Visualizar pasta + cartas */
    @GetMapping("/folders/{id}")
    public ResponseEntity<?> getFolder(@PathVariable Long id, HttpSession session) {
        Long uid = currentUserId(session);

        CollectionFolder f = folderRepo.findByIdAndUserId(id, uid)
                .orElseThrow(() -> new IllegalArgumentException("folder not found"));

        List<CardItem> items = itemRepo.findByFolderIdAndUserId(id, uid);

        // Monta DTO manualmente, normalizando imagePath
        List<Map<String, Object>> itemDtos = items.stream()
                .map(it -> Map.<String, Object>of(
                        "id", it.getId(),
                        "folderId", it.getFolderId(),
                        "userId", it.getUserId(),
                        "cardName", it.getCardName(),
                        "pokemonName", it.getPokemonName(),
                        "source", it.getSource(),
                        "imagePath", normalizeImagePath(it.getImagePath()),
                        "createdAt", it.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "id", f.getId(),
                "name", f.getName(),
                "items", itemDtos
        ));
    }

    @PostMapping("/cards/manual")
    public CardItem addManual(@RequestBody @Valid AddCardManualRequest req, HttpSession session) {
        Long uid = currentUserId(session);
        return service.addManual(uid, req.folderId(), req.cardName());
    }

    @PostMapping(value = "/cards/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CardItem scan(@RequestParam(value = "folderId", required = false) Long folderId,
                        @RequestParam(value = "folderName", required = false) String folderName,
                        @RequestParam("file") MultipartFile file,
                        HttpSession session) throws Exception {
        Long uid = currentUserId(session);

        // se não veio ID, tenta resolver pelo nome da pasta
        if (folderId == null) {
            String name = folderName == null ? "" : folderName.trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("folderId or folderName is required");
            }

            var folderOpt = folderRepo.findByUserIdAndName(uid, name);
            var folder = folderOpt.orElseThrow(() -> new IllegalArgumentException("folder not found"));
            folderId = folder.getId();
        }

        // salva arquivo temporário e chama o OCR
        File tmp = File.createTempFile(
                "card_",
                "_" + (file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename())
        );
        file.transferTo(tmp);
        Path storage = Path.of("data", "users", String.valueOf(uid), "images");
        return service.scanAndAdd(uid, folderId, tmp, storage);
    }


    // renomear pasta
    @PatchMapping("/folders/{id}")
    public ResponseEntity<?> renameFolder(@PathVariable Long id,
                                          @RequestBody Map<String,String> body,
                                          HttpSession session) {
        Long uid = currentUserId(session);
        String newName = body.getOrDefault("name", "").trim();
        if (newName.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","name required"));
        var f = service.renameFolder(uid, id, newName);
        return ResponseEntity.ok(Map.of("id", f.getId(), "name", f.getName()));
    }

    // excluir pasta (apaga cartas dentro)
    @DeleteMapping("/folders/{id}")
    public ResponseEntity<?> deleteFolder(@PathVariable Long id, HttpSession session) {
        Long uid = currentUserId(session);
        service.deleteFolder(uid, id);
        return ResponseEntity.noContent().build();
    }

    // excluir carta
    @DeleteMapping("/cards/{id}")
    public ResponseEntity<?> deleteCard(@PathVariable Long id, HttpSession session) {
        Long uid = currentUserId(session);
        service.deleteCard(uid, id);
        return ResponseEntity.noContent().build();
    }

    // editar nome da carta (pokemonName)
    @PatchMapping("/cards/{id}")
    public ResponseEntity<?> renameCard(@PathVariable Long id,
                                        @RequestBody Map<String,String> body,
                                        HttpSession session) {
        Long uid = currentUserId(session);
        String newName = body.getOrDefault("pokemonName", "").trim();
        if (newName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "pokemonName required"));
        }
        var updated = service.renameCard(uid, id, newName);
        return ResponseEntity.ok(updated);
    }



}
