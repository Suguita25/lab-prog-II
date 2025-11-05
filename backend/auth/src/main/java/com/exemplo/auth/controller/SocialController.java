package com.exemplo.auth.controller;

import com.exemplo.auth.model.DirectMessage;
import com.exemplo.auth.model.Friendship;
import com.exemplo.auth.model.Friendship.Status;
import com.exemplo.auth.repository.UserRepository;
import com.exemplo.auth.service.SocialService;
import jakarta.servlet.http.HttpSession;
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

    public SocialController(SocialService social, UserRepository users) {
        this.social = social;
        this.users = users;
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
    public ResponseEntity<Map<String,String>> onBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /* ----------------------- amigos ------------------------ */

    @PostMapping("/friends/request")
    public ResponseEntity<Friendship> request(@RequestBody Map<String,String> body, HttpSession session) {
        Long uid = currentUserId(session);
        String toEmail = body.getOrDefault("email", "");
        Friendship f = social.requestFriend(uid, toEmail);
        // 200 se já existia, 201 se foi criado agora — aqui retornamos 200 para simplificar
        return ResponseEntity.ok(f);
    }

    /** Solicitações pendentes onde EU sou o destinatário. */
    @GetMapping("/friends/pending")
    public ResponseEntity<List<Friendship>> pending(HttpSession session) {
        return ResponseEntity.ok(social.myPending(currentUserId(session)));
    }

    /** Lista de amigos (status ACCEPTED) em qualquer direção. */
    @GetMapping("/friends")
    public ResponseEntity<List<Friendship>> friends(HttpSession session) {
        return ResponseEntity.ok(social.myFriends(currentUserId(session)));
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
                throw new IllegalArgumentException(" parâmetro 'since' inválido; use ISO-8601, ex: 2025-11-05T02:10:00Z ");
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
            // sempre limpar o arquivo temporário, se criado
            if (tmp != null && tmp.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<Map<String,String>> handleIllegalArg(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
}

@ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
public ResponseEntity<Map<String,String>> handleUnique() {
    return ResponseEntity.status(409).body(Map.of("error", "Solicitação já existe para esse par de usuários."));
}

}
