package com.exemplo.auth.controller;

import com.exemplo.auth.dto.LoginRequest;
import com.exemplo.auth.dto.RegisterRequest;
import com.exemplo.auth.model.User;
import com.exemplo.auth.repository.UserRepository;
import com.exemplo.auth.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;
    private final UserRepository users;

    public AuthController(AuthService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    /* ===== helpers ===== */

    private Map<String, Object> userToPayload(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("email", u.getEmail());
        // pode ser null, sem problema
        map.put("profileImagePath", u.getProfileImagePath());
        return map;
    }

    /* ===== Registro ===== */

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest req,
                                      HttpSession session) {
        User u = service.register(req);

        // já loga o usuário depois de cadastrar
        session.setAttribute("auth", true);
        session.setAttribute("email", u.getEmail());

        return ResponseEntity.ok(userToPayload(u));
    }

    /* ===== Login ===== */

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest req,
                                   HttpSession session) {
        boolean ok = service.login(req);
        if (!ok) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Credenciais inválidas"));
        }

        session.setAttribute("auth", true);
        session.setAttribute("email", req.email());

        User u = users.findByEmail(req.email()).orElseThrow();
        return ResponseEntity.ok(userToPayload(u));
    }

    /* ===== Logout ===== */

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("status", "BYE"));
    }

    /* ===== /me ===== */

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Boolean auth = (Boolean) session.getAttribute("auth");
        if (auth == null || !auth) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "não autenticado"));
        }

        String email = (String) session.getAttribute("email");
        User u = users.findByEmail(email).orElseThrow();

        return ResponseEntity.ok(userToPayload(u));
    }

    /* ===== Tratamento de erros ===== */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegal(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleUnique(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Email ou usuário já cadastrados."));
    }
}
