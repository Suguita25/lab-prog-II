package com.exemplo.auth.service;

import com.exemplo.auth.model.CardItem;
import com.exemplo.auth.model.CollectionFolder;
import com.exemplo.auth.repository.CardItemRepository;
import com.exemplo.auth.repository.CollectionFolderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CollectionService {

    private final CollectionFolderRepository folderRepo;
    private final CardItemRepository itemRepo;
    private final PokemonDictionary dict; // seu dicionário existente
    private final OcrService ocr;         // seu serviço de OCR

    public CollectionService(CollectionFolderRepository folderRepo,
                             CardItemRepository itemRepo,
                             PokemonDictionary dict,
                             OcrService ocr) {
        this.folderRepo = folderRepo;
        this.itemRepo = itemRepo;
        this.dict = dict;
        this.ocr = ocr;
    }

    /* ==================== Pastas ==================== */

    public CollectionFolder createFolder(Long userId, String name) {
        CollectionFolder f = new CollectionFolder();
        f.setUserId(userId);
        f.setName(name);
        f.setCreatedAt(Instant.now()); // <<<< Instant, não OffsetDateTime
        return folderRepo.save(f);
    }

    public List<CollectionFolder> listFolders(Long userId) {
        return folderRepo.findByUserId(userId);
    }

    @Transactional
    public CollectionFolder renameFolder(Long userId, Long folderId, String newName) {
        CollectionFolder f = folderRepo.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("folder not found"));
        f.setName(newName);
        return folderRepo.save(f);
    }

    @Transactional
    public void deleteFolder(Long userId, Long folderId) {
        // Sem deleteByFolderIdAndUserId: buscamos e deletamos em lote
        List<CardItem> items = itemRepo.findByFolderIdAndUserId(folderId, userId);
        itemRepo.deleteAll(items);

        CollectionFolder f = folderRepo.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("folder not found"));
        folderRepo.delete(f);
    }

    /* ==================== Cartas ==================== */

    public CardItem addManual(Long userId, Long folderId, String cardName) {
        CollectionFolder f = folderRepo.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("folder not found"));

        CardItem item = new CardItem();
        item.setFolderId(f.getId());
        item.setUserId(userId);
        item.setCardName(cardName);
        item.setPokemonName(dict.bestMatch(cardName).orElse(null));
        item.setSource("manual");
        item.setCreatedAt(Instant.now()); // <<<< Instant
        return itemRepo.save(item);
    }

    public CardItem scanAndAdd(Long userId, Long folderId, File tempImage, Path storageBase) throws Exception {
        CollectionFolder f = folderRepo.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("folder not found"));

        // OCR
        String raw = ocr.extractText(tempImage);

        // Heurística: tenta escolher o melhor candidato do texto OCR usando o dicionário existente
        String candidate = guessCardName(raw);
        String normalized = dict.bestMatch(candidate).orElse(candidate);

        // Salva a imagem
        Files.createDirectories(storageBase);
        Path target = storageBase.resolve(tempImage.getName());
        Files.move(tempImage.toPath(), target);

        CardItem item = new CardItem();
        item.setFolderId(f.getId());
        item.setUserId(userId);
        item.setCardName(candidate);
        item.setPokemonName(normalized);
        item.setSource("ocr");
        item.setImagePath(target.toString());
        item.setCreatedAt(Instant.now()); // <<<< Instant
        return itemRepo.save(item);
    }

    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        // Sem existsByIdAndUserId: valida com findById + checagem de userId
        CardItem it = itemRepo.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("card not found"));
        if (!Objects.equals(it.getUserId(), userId)) {
            throw new IllegalArgumentException("card not found");
        }
        itemRepo.deleteById(cardId);
    }

    /* ==================== Helpers ==================== */

    // Tenta achar a melhor palavra/frase do OCR que bata no dicionário
    private String guessCardName(String raw) {
        if (raw == null || raw.isBlank()) return "Unknown";

        // normaliza: tira caracteres estranhos, quebra por linhas e espaços
        String cleaned = raw.replaceAll("[^\\p{L}\\p{Nd} \\-\\n]", " ").toLowerCase(Locale.ROOT);
        String[] lines = cleaned.split("\\R+");

        // 1) tenta cada linha inteira no bestMatch
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() < 2) continue;
            var m = dict.bestMatch(trimmed);
            if (m.isPresent()) return m.get();
        }

        // 2) tenta tokens individuais
        Pattern split = Pattern.compile("[\\s\\-]+");
        for (String line : lines) {
            String[] toks = split.split(line.trim());
            for (String t : toks) {
                if (t.length() < 2) continue;
                var m = dict.bestMatch(t);
                if (m.isPresent()) return m.get();
            }
        }

        // 3) fallback
        return "Unknown";
    }
}
