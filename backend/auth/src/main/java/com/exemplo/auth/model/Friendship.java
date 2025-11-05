package com.exemplo.auth.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.Check;

@Entity
@Table(
    name = "friendships",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_friend_pair",
        columnNames = {"requester_id","addressee_id"}
    ),
    indexes = {
        @Index(name = "idx_friendships_requester", columnList = "requester_id"),
        @Index(name = "idx_friendships_addressee", columnList = "addressee_id")
    }
)
// impede amizade com o próprio usuário (PostgreSQL/Hibernate entenderão a CHECK)
@Check(constraints = "requester_id <> addressee_id")
public class Friendship {

    public enum Status { PENDING, ACCEPTED, DECLINED, BLOCKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "addressee_id", nullable = false)
    private Long addresseeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /* ---- JPA lifecycle ---- */
    @PrePersist
    void onCreate() {
        final Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /* ---- Getters/Setters ---- */
    public Long getId() { return id; }

    public Long getRequesterId() { return requesterId; }
    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }

    public Long getAddresseeId() { return addresseeId; }
    public void setAddresseeId(Long addresseeId) { this.addresseeId = addresseeId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    /* ---- Helpers ---- */

    /** Retorna true se o par (a,b) corresponde a esta relação em qualquer direção. */
    public boolean involves(Long a, Long b) {
        return (Objects.equals(requesterId, a) && Objects.equals(addresseeId, b)) ||
               (Objects.equals(requesterId, b) && Objects.equals(addresseeId, a));
    }

    /** Dado um userId participante, retorna o id do outro; senão, null. */
    public Long otherOf(Long userId) {
        if (Objects.equals(requesterId, userId)) return addresseeId;
        if (Objects.equals(addresseeId, userId)) return requesterId;
        return null;
    }

    /* ---- Equality por id ---- */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Friendship f)) return false;
        return id != null && id.equals(f.id);
    }

    @Override
    public int hashCode() { return 31; }
}
