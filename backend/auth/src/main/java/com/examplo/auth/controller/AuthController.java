package com.exemplo.auth.controller;

import com.exemplo.auth.dto.LoginRequest;
import com.exemplo.auth.dto.RegisterRequest;
import com.exemplo.auth.model.User;
import com.exemplo.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest req) {
        User u = service.register(req);
        // Retorna dados básicos (sem senha)
        return ResponseEntity.ok(new Object() {
            public final Long id = u.getId();
            public final String username = u.getUsername();
            public final String email = u.getEmail();
        });
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest req) {
        boolean ok = service.login(req);
        if (ok) {
            // Simples: só confirma. (Se quiser, depois colocamos JWT/sessão.)
            return ResponseEntity.ok(new Object() { public final String status = "OK"; });
        }
        return ResponseEntity.status(401).body(new Object() { public final String error = "Credenciais inválidas"; });
    }
}

