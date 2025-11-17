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

    /** Relações ACCEPTED envolvendo o usuário em qualquer direção. */
    @Transactional(readOnly = true)
    public List<Friendship> myFriends(Long userId) {
        // Se você manteve os métodos antigos, substitua por:
        // return friends.findByRequesterIdOrAddresseeIdAndStatus(userId, userId, Status.ACCEPTED);
        return friends.findFriendsOf(userId);
        //return friends.findAllOfUserWithStatus(userId, Status.ACCEPTED);
    }

        /** Lista de amigos com dados do outro usuário (id + username + email). */
    @Transactional(readOnly = true)
    public List<FriendView> myFriendViews(Long userId) {
        // amizades ACCEPTED que envolvem o usuário
        List<Friendship> fs = friends.findFriendsOf(userId);
        if (fs.isEmpty()) return List.of();

        // pega todos os IDs dos "outros" usuários
        Set<Long> otherIds = new HashSet<>();
        for (Friendship f : fs) {
            Long other = f.otherOf(userId);
            if (other != null) {
                otherIds.add(other);
            }
        }
        if (otherIds.isEmpty()) return List.of();

        // carrega todos os usuários em um único select
        Map<Long, User> userMap = users.findAllById(otherIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // monta a lista de FriendView
        List<FriendView> views = new ArrayList<>();
        for (Friendship f : fs) {
            Long other = f.otherOf(userId);
            if (other == null) continue;
            User u = userMap.get(other);
            if (u == null) continue;

            views.add(new FriendView(
                    f.getId(),       // friendshipId
                    other,           // userId do amigo
                    u.getUsername(), // username
                    u.getEmail()     // email (fallback se precisar)
            ));
        }
        return views;
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
