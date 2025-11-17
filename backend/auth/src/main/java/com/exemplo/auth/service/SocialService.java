package com.exemplo.auth.service;

import com.exemplo.auth.model.DirectMessage;
import com.exemplo.auth.model.Friendship;
import com.exemplo.auth.model.Friendship.Status;
import com.exemplo.auth.model.User;
import com.exemplo.auth.repository.DirectMessageRepository;
import com.exemplo.auth.repository.FriendshipRepository;
import com.exemplo.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.exemplo.auth.dto.PendingFriendView;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import com.exemplo.auth.dto.FriendView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class SocialService {

    private final FriendshipRepository friends;
    private final DirectMessageRepository msgs;
    private final UserRepository users;

    public SocialService(FriendshipRepository friends,
                         DirectMessageRepository msgs,
                         UserRepository users) {
        this.friends = friends;
        this.msgs = msgs;
        this.users = users;
    }

    /* ===================== Friends ===================== */

    @Transactional
public Friendship requestFriend(Long requesterId, String email) {
    var to = users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

    if (requesterId.equals(to.getId())) {
        throw new IllegalArgumentException("Você não pode adicionar você mesmo.");
    }

    // se já existe (pendente ou aceito), devolve o existente
    var existing = friends.findAnyDirection(requesterId, to.getId());
    if (existing.isPresent()) return existing.get();

    var f = new Friendship();
    f.setRequesterId(requesterId);
    f.setAddresseeId(to.getId());
    f.setStatus(Status.PENDING);
    return friends.save(f);
}

    @Transactional
    public Friendship changeStatus(Long actorId, Long friendshipId, Status newStatus) {
        Friendship f = friends.findById(friendshipId).orElseThrow();

        // regras:
        // - ACCEPTED/DECLINED: somente o destinatário (addressee) pode decidir
        // - BLOCKED: qualquer participante pode bloquear
        // - PENDING: só faz sentido quando criado; aqui não reabrimos para PENDING
        boolean isAddressee = Objects.equals(actorId, f.getAddresseeId());
        boolean isRequester = Objects.equals(actorId, f.getRequesterId());
        if (!(isAddressee || isRequester)) {
            throw new IllegalArgumentException("sem permissão");
        }

        if (newStatus == Status.ACCEPTED || newStatus == Status.DECLINED) {
            if (!isAddressee) throw new IllegalArgumentException("apenas o destinatário pode aceitar/recusar");
        }

        f.setStatus(newStatus);
        return friends.save(f);
    }

    /** Solicitações pendentes onde EU sou o destinatário. */
    @Transactional(readOnly = true)
    public List<Friendship> myPending(Long userId) {
        return friends.findByAddresseeIdAndStatus(userId, Status.PENDING);
    }

    /** Solicitações pendentes (onde EU sou o destinatário) com dados do remetente. */
    @Transactional(readOnly = true)
    public List<PendingFriendView> myPendingViews(Long userId) {
        List<Friendship> list = friends.findByAddresseeIdAndStatus(userId, Status.PENDING);
        if (list.isEmpty()) return List.of();

        // pega todos os requesterIds
        Set<Long> requesterIds = new HashSet<>();
        for (Friendship f : list) {
            requesterIds.add(f.getRequesterId());
        }

        Map<Long, User> userMap = users.findAllById(requesterIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<PendingFriendView> out = new ArrayList<>();
        for (Friendship f : list) {
            User u = userMap.get(f.getRequesterId());
            if (u == null) continue; // em caso de usuário apagado

            out.add(new PendingFriendView(
                    f.getId(),          // friendshipId
                    u.getId(),          // requesterId
                    u.getUsername(),    // requesterUsername
                    u.getEmail()        // requesterEmail
            ));
        }
        return out;
    }


     /** Relações ACCEPTED envolvendo o usuário em qualquer direção. */
    @Transactional(readOnly = true)
    public List<Friendship> myFriends(Long userId) {
        return friends.findFriendsOf(userId);
    }

    /** Versão com dados do “outro” usuário (username, email, avatar). */
    @Transactional(readOnly = true)
    public List<FriendView> myFriendViews(Long userId) {
        List<Friendship> rels = myFriends(userId);
        if (rels.isEmpty()) return List.of();

        Set<Long> others = new HashSet<>();
        for (Friendship f : rels) {
            Long other = f.otherOf(userId);
            if (other != null) others.add(other);
        }

        Map<Long, User> userMap = users.findAllById(others).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<FriendView> out = new ArrayList<>();
        for (Friendship f : rels) {
            Long otherId = f.otherOf(userId);
            if (otherId == null) continue;
            User u = userMap.get(otherId);
            if (u == null) continue;

            out.add(new FriendView(
                    f.getId(),
                    u.getId(),
                    u.getUsername(),
                    u.getEmail(),
                    u.getProfileImagePath()
            ));
        }
        return out;
    }

    @Transactional
    public void removeFriendship(Long actorId, Long friendshipId) {
        Friendship f = friends.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("amizade não encontrada"));

        boolean isParticipant =
                Objects.equals(actorId, f.getRequesterId()) ||
                Objects.equals(actorId, f.getAddresseeId());

        if (!isParticipant) {
            throw new IllegalArgumentException("sem permissão para remover esta amizade");
        }

        friends.delete(f);
    }




    /** Verifica se dois usuários são amigos (ACCEPTED) em qualquer direção. */
    @Transactional(readOnly = true)
    public boolean isFriends(Long a, Long b) {
        return friends.findFriendsOf(a).stream().anyMatch(fr -> fr.involves(a, b) && fr.getStatus() == Status.ACCEPTED);
    }

    /* ===================== Messages ===================== */

    @Transactional
    public DirectMessage sendMessage(Long from, Long to, String text, File image, Path storageRoot) throws Exception {
        // exige amizade ACCEPTED em qualquer direção
        if (!isFriends(from, to))
            throw new IllegalArgumentException("vocês não são amigos");

        DirectMessage dm = new DirectMessage();
        dm.setSenderId(from);
        dm.setReceiverId(to);
        dm.setText(text != null && !text.isBlank() ? text : null);
        dm.setCreatedAt(Instant.now());

        if (image != null) {
            Files.createDirectories(storageRoot);
            String ext = extOf(image.getName());
            String safeName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
            Path target = storageRoot.resolve(safeName);
            // copy em vez de move para evitar problemas de cross-device
            Files.copy(image.toPath(), target);
            dm.setImagePath(target.toString());
        }

        return msgs.save(dm);
    }

    @Transactional(readOnly = true)
    public List<DirectMessage> history(Long a, Long b) {
        // Se você criou @Query com parênteses corretos, use algo como msgs.findDialog(a,b)
        // Caso contrário, mantém o método derivado já existente:
        return msgs.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(a, b, b, a);
    }

    @Transactional(readOnly = true)
    public List<DirectMessage> since(Long a, Long b, Instant after) {
        // Idem ao de cima — ideal é ter @Query com parênteses; mantido método atual:
        return msgs.findByCreatedAtAfterAndSenderIdAndReceiverIdOrCreatedAtAfterAndSenderIdAndReceiverIdOrderByCreatedAtAsc(
                after, a, b, after, b, a);
    }

    /* ===================== helpers ===================== */

    private static String extOf(String filename) {
        int i = filename.lastIndexOf('.');
        if (i <= 0 || i == filename.length() - 1) return "";
        return filename.substring(i + 1).replaceAll("[^A-Za-z0-9]", "");
    }
}
