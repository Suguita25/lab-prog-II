package com.exemplo.auth.controller;

import com.exemplo.auth.model.User;
import com.exemplo.auth.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository users;

    public ProfileController(UserRepository users) {
        this.users = users;
    }

    private Long currentUserId(HttpSession session) {
        Boolean auth = (Boolean) session.getAttribute("auth");
        if (auth == null || !auth) throw new Unauthorized();
        String email = (String) session.getAttribute("email");
        return users.findByEmail(email).orElseThrow().getId();
    }

    @ResponseStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
    static class Unauthorized extends RuntimeException {}

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String,String>> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                           HttpSession session) throws Exception {
        Long uid = currentUserId(session);
        User u = users.findById(uid).orElseThrow();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de imagem obrigatório.");
        }

        String original = file.getOriginalFilename();
        String ext = ".png";
        if (original != null && original.lastIndexOf('.') > 0) {
            String e = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (e.matches("\\.(png|jpg|jpeg|webp|gif|bmp)$")) {
                ext = e;
            }
        }

        Path dir = Path.of("data", "users", String.valueOf(uid), "avatar");
        Files.createDirectories(dir);
        String filename = "avatar" + ext;
        Path target = dir.resolve(filename);

        file.transferTo(target);

        // caminho servido pelo seu FileController (igual das cartas)
        String url = "/files/users/" + uid + "/avatar/" + filename;
        u.setProfileImagePath(url);
        users.save(u);

        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> handleBadReq(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

