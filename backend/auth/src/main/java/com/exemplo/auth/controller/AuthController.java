package com.exemplo.auth.controller;

import com.exemplo.auth.dto.LoginRequest;
import com.exemplo.auth.dto.RegisterRequest;
import com.exemplo.auth.model.User;
import com.exemplo.auth.repository.UserRepository;
import com.exemplo.auth.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest req) {
        User u = service.register(req);
        // retorna dados básicos (sem senha)
        return ResponseEntity.ok(Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "email", u.getEmail()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest req, HttpSession session) {
        boolean ok = service.login(req);
        if (!ok) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciais inválidas"));
        }

        // cria a sessão do usuário
        session.setAttribute("auth", true);
        session.setAttribute("email", req.email());
        // se mais tarde você tiver o id do usuário aqui, pode fazer:
        // session.setAttribute("uid", userId);

        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("status", "BYE"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Boolean auth = (Boolean) session.getAttribute("auth");
        if (auth == null || !auth) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        String email = (String) session.getAttribute("email");
        User u = users.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(Map.of(
                "id", u.getId(),
                "email", u.getEmail(),
                "username", u.getUsername(),
                "profileImagePath", u.getProfileImagePath()  
        ));
    }
}
