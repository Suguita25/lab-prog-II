package com.exemplo.auth.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name="uk_users_email", columnNames = "email"),
        @UniqueConstraint(name="uk_users_username", columnNames = "username")
})
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 40)
    private String username;

    @NotBlank @Email
    @Column(nullable = false, length = 120)
    private String email;

    @NotBlank
    @Column(nullable = false, length = 60) // hash bcrypt ~60 chars
    private String passwordHash;

    // getters e setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}

