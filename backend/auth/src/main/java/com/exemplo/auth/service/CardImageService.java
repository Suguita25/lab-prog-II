package com.exemplo.auth.service;


import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class CardImageService {

    private final Path rootDir = Paths.get("data"); // mesma pasta do print

    public String saveUserCardImage(Long userId, MultipartFile file) throws IOException {
        // data/users/{id}/images
        Path userImagesDir = rootDir
                .resolve("users")
                .resolve(String.valueOf(userId))
                .resolve("images");

        Files.createDirectories(userImagesDir);

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = "card_" + System.currentTimeMillis() + ext;
        Path target = userImagesDir.resolve(fileName);

        file.transferTo(target.toFile());

        // ESTA URL é o que vai no JSON como imagePath
        return "/files/users/" + userId + "/images/" + fileName;
    }
}

