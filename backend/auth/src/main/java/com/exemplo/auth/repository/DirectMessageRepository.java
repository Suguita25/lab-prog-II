package com.exemplo.auth.repository;

import com.exemplo.auth.model.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    List<DirectMessage> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
            Long s1, Long r1, Long s2, Long r2);

    List<DirectMessage> findByCreatedAtAfterAndSenderIdAndReceiverIdOrCreatedAtAfterAndSenderIdAndReceiverIdOrderByCreatedAtAsc(
            Instant after1, Long s1, Long r1, Instant after2, Long s2, Long r2);
}

