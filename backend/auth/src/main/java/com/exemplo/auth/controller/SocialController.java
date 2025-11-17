package com.exemplo.auth.controller;

import com.exemplo.auth.dto.FriendView;
import com.exemplo.auth.model.CardItem;
import com.exemplo.auth.model.CollectionFolder;
import com.exemplo.auth.model.DirectMessage;
import com.exemplo.auth.model.Friendship;
import com.exemplo.auth.model.Friendship.Status;
import com.exemplo.auth.repository.CardItemRepository;
import com.exemplo.auth.repository.CollectionFolderRepository;
import com.exemplo.auth.repository.UserRepository;
import com.exemplo.auth.service.SocialService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final SocialService social;
    private final UserRepository users;
    private final CollectionFolderRepository folders;
    private final CardItemRepository cards;

    public SocialController(SocialService social,
                            UserRepository users,
                            CollectionFolderRepository folders,
                            CardItemRepository cards) {
        this.social = social;
        this.users = users;
        this.folders = folders;
        this.cards = cards;
    }

    /* --------------------- util sessão --------------------- */

    private Long currentUserId(HttpSession session) {
        Boolean auth = (Boolean) session.getAttribute("auth");
        if (auth == null || !auth) throw new Unauthorized();
        String email = (String) session.getAttribute("email");
        return users.findByEmail(email).orElseThrow().getId();
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class Unauthorized extends RuntimeException {}

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,String>> handleUnique() {
        return ResponseEntity.status(409)
                .body(Map.of("error", "Solicitação já existe para esse par de usuários."));
    }

    /* ----------------------- amigos ------------------------ */

    @PostMapping("/friends/request")
    public ResponseEntity<Friendship> request(@RequestBody Map<String,String> body, HttpSession session) {
        Long uid = currentUserId(session);
        String toEmail = body.getOrDefault("email", "");
        Friendship f = social.requestFriend(uid, toEmail);
        return ResponseEntity.ok(f);
    }

    /** Solicitações pendentes onde EU sou o destinatário. */
    @GetMapping("/friends/pending")
    public ResponseEntity<List<Friendship>> pending(HttpSession session) {
        return ResponseEntity.ok(social.myPending(currentUserId(session)));
    }

    /** Lista de amigos (status ACCEPTED) em qualquer direção, com username/email. */
    @GetMapping("/friends")
    public ResponseEntity<List<FriendView>> friends(HttpSession session) {
        Long uid = currentUserId(session);
        return ResponseEntity.ok(social.myFriendViews(uid));
    }

    @PatchMapping("/friends/{id}/accept")
    public ResponseEntity<Friendship> accept(@PathVariable Long id, HttpSession session) {
        Friendship f = social.changeStatus(currentUserId(session), id, Status.ACCEPTED);
        return ResponseEntity.ok(f);
    }

    @PatchMapping("/friends/{id}/decline")
    public ResponseEntity<Friendship> decline(@PathVariable Long id, HttpSession session) {
        Friendship f = social.changeStatus(currentUserId(session), id, Status.DECLINED);
        return ResponseEntity.ok(f);
    }

    /* --------- visualizar coleções do amigo (somente leitura) --------- */

    /** Lista de pastas do amigo (somente se forem amigos). */
    @GetMapping("/friends/{friendId}/folders")
    public ResponseEntity<List<CollectionFolder>> friendFolders(@PathVariable Long friendId,
                                                                HttpSession session) {
        Long me = currentUserId(session);
        if (!social.isFriends(me, friendId)) {
            throw new IllegalArgumentException("vocês não são amigos");
        }
        List<CollectionFolder> list = folders.findByUserId(friendId);
        return ResponseEntity.ok(list);
    }

    /** Conteúdo de uma pasta do amigo (somente leitura). */
    @GetMapping("/friends/{friendId}/folders/{folderId}")
    public ResponseEntity<Map<String, Object>> friendFolderDetail(@PathVariable Long friendId,
                                                                  @PathVariable Long folderId,
                                                                  HttpSession session) {
        Long me = currentUserId(session);
        if (!social.isFriends(me, friendId)) {
            throw new IllegalArgumentException("vocês não são amigos");
        }

        CollectionFolder f = folders.findByIdAndUserId(folderId, friendId)
                .orElseThrow(() -> new IllegalArgumentException("pasta não encontrada"));

        List<CardItem> items = cards.findByFolderIdAndUserId(folderId, friendId);

        return ResponseEntity.ok(Map.of(
                "id", f.getId(),
                "name", f.getName(),
                "items", items
        ));
    }

    /* ---------------------- mensagens ---------------------- */

    @GetMapping("/messages")
    public ResponseEntity<List<DirectMessage>> history(@RequestParam Long withUserId,
                                                       @RequestParam(required = false) String since,
                                                       HttpSession session) {
        Long me = currentUserId(session);
        if (since != null && !since.isBlank()) {
            try {
                Instant t = Instant.parse(since);
                return ResponseEntity.ok(social.since(me, withUserId, t));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("parâmetro 'since' inválido; use ISO-8601, ex: 2025-11-05T02:10:00Z");
            }
        }
        return ResponseEntity.ok(social.history(me, withUserId));
    }

    @PostMapping(value = "/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DirectMessage> send(@RequestParam Long toUserId,
                                              @RequestParam(required = false) String text,
                                              @RequestParam(value = "file", required = false) MultipartFile file,
                                              HttpSession session) throws Exception {
        Long me = currentUserId(session);

        File tmp = null;
        try {
            if (file != null && !file.isEmpty()) {
                String original = file.getOriginalFilename();
                tmp = File.createTempFile("dm_", "_" + (original == null ? "upload" : original));
                file.transferTo(tmp);
            }
            Path storage = Path.of("data", "users", String.valueOf(me), "messages");
            DirectMessage dm = social.sendMessage(me, toUserId, text, tmp, storage);
            return ResponseEntity.status(HttpStatus.CREATED).body(dm);
        } finally {
            if (tmp != null && tmp.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        }
    }
}
