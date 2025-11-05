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
        if (users.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }
        if (users.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username já cadastrado");
        }
        String salt = BCrypt.gensalt(12);
        String hash = BCrypt.hashpw(req.password(), salt);

        User u = new User();
        u.setUsername(req.username());
        u.setEmail(req.email());
        u.setPasswordHash(hash);
        return users.save(u);
    }

    @Transactional(readOnly = true)
    public boolean login(LoginRequest req) {
        return users.findByEmail(req.email())
                .map(u -> BCrypt.checkpw(req.password(), u.getPasswordHash()))
                .orElse(false);
    }
}

