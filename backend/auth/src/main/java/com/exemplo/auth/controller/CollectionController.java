// controller/CollectionController.java
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

    // -------- endpoints --------

    @PostMapping("/folders")
    public ResponseEntity<?> createFolder(@RequestBody @Valid CreateFolderRequest req, HttpSession session) {
        Long uid = currentUserId(session);
        CollectionFolder f = service.createFolder(uid, req.name());
        return ResponseEntity.ok(Map.of("id", f.getId(), "name", f.getName()));
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
        return ResponseEntity.ok(Map.of(
                "id", f.getId(),
                "name", f.getName(),
                "items", items
        ));
    }

    @PostMapping("/cards/manual")
    public CardItem addManual(@RequestBody @Valid AddCardManualRequest req, HttpSession session) {
        Long uid = currentUserId(session);
        return service.addManual(uid, req.folderId(), req.cardName());
    }

    @PostMapping(value = "/cards/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CardItem scan(@RequestParam Long folderId,
                         @RequestParam("file") MultipartFile file,
                         HttpSession session) throws Exception {
        Long uid = currentUserId(session);
        // salva arquivo temporário e chama o OCR
        File tmp = File.createTempFile("card_", "_" + (file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename()));
        file.transferTo(tmp);
        Path storage = Path.of("data", "users", String.valueOf(uid), "images");
        return service.scanAndAdd(uid, folderId, tmp, storage);
    }

/* 
// adiciona carta com nome + imagem (opcional)
@PostMapping(value = "/cards", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public CardItem addCard(
        @RequestParam Long folderId,
        @RequestParam String cardName,
        @RequestParam(value = "file", required = false) MultipartFile file,
        HttpSession session
) throws Exception {
    Long uid = currentUserId(session);
    if (file != null && !file.isEmpty()) {
        File tmp = File.createTempFile("card_", "_" + (file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename()));
        file.transferTo(tmp);
        Path storage = Path.of("data", "users", String.valueOf(uid), "images");
        return service.addWithOptionalImage(uid, folderId, cardName, tmp, storage);
    } else {
        // sem imagem, apenas cadastra com o nome informado
        return service.addManual(uid, folderId, cardName);
    }
}
*/


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
}
