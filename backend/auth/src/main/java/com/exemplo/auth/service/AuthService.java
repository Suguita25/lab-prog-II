package com.exemplo.auth.service;

import com.exemplo.auth.dto.LoginRequest;
import com.exemplo.auth.dto.RegisterRequest;
import com.exemplo.auth.model.User;
import com.exemplo.auth.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public User register(RegisterRequest req) {
        String username = req.username() == null ? "" : req.username().trim();
        String emailRaw = req.email() == null ? "" : req.email().trim();
        String password = req.password() == null ? "" : req.password().trim();

        if (username.isEmpty() || emailRaw.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Preencha usuário, email e senha.");
        }

        String email = emailRaw.toLowerCase(); // 👈 padroniza email

        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email já cadastrado");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Username já cadastrado");
        }

        String salt = BCrypt.gensalt(12);
        String hash = BCrypt.hashpw(password, salt);

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(hash);
        // u.setProfileImagePath(null); // se tiver o campo

        return users.save(u);
    }

    @Transactional(readOnly = true)
    public boolean login(LoginRequest req) {
        String emailRaw = req.email() == null ? "" : req.email().trim();
        String password = req.password() == null ? "" : req.password().trim();

        if (emailRaw.isEmpty() || password.isEmpty()) {
            return false;
        }

        String email = emailRaw.toLowerCase();

        return users.findByEmailIgnoreCase(email)
                .map(u -> BCrypt.checkpw(password, u.getPasswordHash()))
                .orElse(false);
    }
}
