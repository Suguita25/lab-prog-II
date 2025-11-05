package com.exemplo.auth.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "collection_folders",
       uniqueConstraints = @UniqueConstraint(name="uk_folder_user_name", columnNames={"userId","name"}))
public class CollectionFolder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long userId;

    @Column(nullable=false, length=60)
    private String name;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    // getters/setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}


