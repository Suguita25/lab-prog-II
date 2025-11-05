package com.exemplo.auth.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="direct_messages", indexes = {
        @Index(name="ix_dm_pair_time", columnList="sender_id, receiver_id, created_at")
})
public class DirectMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="sender_id", nullable=false)
    private Long senderId;

    @Column(name="receiver_id", nullable=false)
    private Long receiverId;

    @Column(name="text", length=2000)
    private String text;

    @Column(name="image_path")
    private String imagePath;

    @Column(name="created_at", nullable=false)
    private Instant createdAt = Instant.now();

    // getters/setters
    public Long getId(){ return id; }
    public Long getSenderId(){ return senderId; }
    public void setSenderId(Long v){ senderId = v; }
    public Long getReceiverId(){ return receiverId; }
    public void setReceiverId(Long v){ receiverId = v; }
    public String getText(){ return text; }
    public void setText(String t){ text=t; }
    public String getImagePath(){ return imagePath; }
    public void setImagePath(String p){ imagePath=p; }
    public Instant getCreatedAt(){ return createdAt; }
    public void setCreatedAt(Instant t){ createdAt=t; }
}
