package com.exemplo.auth.repository;

import com.exemplo.auth.model.Friendship;
import com.exemplo.auth.model.Friendship.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** Direção exata (A -> B). Útil para saber quem solicitou. */
    Optional<Friendship> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    /** Par em QUALQUER direção (A <-> B). Evita duplicatas / checagem de existência. */
    @Query("""
        select f from Friendship f
         where (f.requesterId = :a and f.addresseeId = :b)
            or (f.requesterId = :b and f.addresseeId = :a)
    """)
    Optional<Friendship> findPair(Long a, Long b);

    /** Existe par em qualquer direção? */
    @Query("""
        select case when count(f) > 0 then true else false end
          from Friendship f
         where (f.requesterId = :a and f.addresseeId = :b)
            or (f.requesterId = :b and f.addresseeId = :a)
    """)
    boolean existsPair(Long a, Long b);

    /** Pendentes onde EU sou o destinatário (quem precisa aceitar sou eu). */
    List<Friendship> findByAddresseeIdAndStatus(Long addresseeId, Status status);

    /** Todas as minhas relações com um dado status (em qualquer direção). */
    @Query("""
        select f from Friendship f
         where (f.requesterId = :userId or f.addresseeId = :userId)
           and f.status = :status
         order by f.id desc
    """)
    List<Friendship> findByUserAndStatus(Long userId, Status status);

    /** Meus amigos (status ACCEPTED) em qualquer direção. */
    @Query("""
        select f from Friendship f
         where (f.requesterId = :userId or f.addresseeId = :userId)
           and f.status = 'ACCEPTED'
         order by f.id desc
    """)
    List<Friendship> findFriendsOf(Long userId);

    /** Alias do par em qualquer direção (se precisar reaproveitar). */
    @Query("""
      select f from Friendship f
      where (f.requesterId = :a and f.addresseeId = :b)
         or (f.requesterId = :b and f.addresseeId = :a)
    """)
    Optional<Friendship> findAnyDirection(Long a, Long b);
}
