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
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class CollectionService {

    private final CollectionFolderRepository folderRepo;
    private final CardItemRepository itemRepo;
    private final PokemonDictionary dict; // dicionário existente
    private final OcrService ocr;         // serviço de OCR via Tesseract CLI

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
        f.setCreatedAt(Instant.now());
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
        item.setPokemonName(dict.bestMatch(cardName).orElse(cardName));
        item.setSource("manual");
        item.setCreatedAt(Instant.now());
        return itemRepo.save(item);
    }

    /**
     * Usa OCR para extrair o nome da carta a partir da imagem e salva o item.
     */
    public CardItem scanAndAdd(Long userId, Long folderId, File tempImage, Path storageBase) throws Exception {
        var f = folderRepo.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("folder not found"));

        // 1) OCR focado no título
        String candidate = null;
        try {
            String ocrName = ocr.extractCardName(tempImage);
            if (ocrName != null) {
                ocrName = ocrName.trim();
                if (!ocrName.isBlank()) candidate = ocrName;
            }
        } catch (Exception ignore) {
            // continua nos fallbacks
        }

        // 2) Fallback: OCR completo + heurística
        if (candidate == null || candidate.isBlank()) {
            try {
                String raw = ocr.extractText(tempImage);
                if (raw != null) {
                    String g = guessCardName(raw);
                    if (g != null && !g.isBlank()) candidate = g.trim();
                }
            } catch (Exception ignore) { }
        }

        // 3) Fallback extra: tentar deduzir pelo nome do arquivo
        if ((candidate == null || candidate.isBlank()) && tempImage.getName() != null) {
            String fname = tempImage.getName()
                    .replace('_', ' ')
                    .replace('-', ' ')
                    .replaceAll("(?i)\\.(png|jpe?g|webp|bmp)$", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (!fname.isBlank()) {
                var bm = dict.bestMatch(fname);
                if (bm.isPresent()) {
                    candidate = bm.get();
                } else {
                    String[] toks = fname.split(" ");
                    java.util.Arrays.sort(toks, (a, b) -> Integer.compare(b.length(), a.length()));
                    for (String t : toks) {
                        if (t.isBlank()) continue;
                        var mt = dict.bestMatch(t);
                        if (mt.isPresent()) { candidate = mt.get(); break; }
                    }
                }
            }
        }

        // 4) Normalização final via dicionário
        String normalized = "Unknown";
        if (candidate != null && !candidate.isBlank()) {
            var mFull = dict.bestMatch(candidate);
            if (mFull.isPresent()) {
                normalized = mFull.get();
            } else {
                String[] toks = candidate.split("\\s+");
                for (String t : toks) {
                    if (t.isBlank()) continue;
                    var mt = dict.bestMatch(t);
                    if (mt.isPresent()) { normalized = mt.get(); break; }
                }
            }
        }

        // 5) Salva a imagem no storage do usuário, garantindo nome único
        Files.createDirectories(storageBase);
        String baseName = tempImage.getName();
        String ext = baseName.contains(".") ? baseName.substring(baseName.lastIndexOf('.')) : ".png";
        String safeName = "card_" + System.currentTimeMillis() + "_" + Math.abs(baseName.hashCode()) + ext;
        Path target = storageBase.resolve(safeName);
        Files.move(tempImage.toPath(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // *** IMPORTANTE: gera URL pública a partir do caminho físico ***
        String publicUrl = toPublicImageUrl(target);

        // 6) Persiste
        CardItem item = new CardItem();
        item.setFolderId(f.getId());
        item.setUserId(userId);
        item.setCardName((candidate == null || candidate.isBlank()) ? normalized : candidate);
        item.setPokemonName(normalized);
        item.setSource("ocr");
        item.setImagePath(publicUrl);  // <--- agora é URL /files/...
        item.setCreatedAt(Instant.now());

        return itemRepo.save(item);
    }

    /**
     * Adiciona manualmente com imagem opcional (se tempImage != null).
     */
    public CardItem addWithOptionalImage(Long userId, Long folderId, String cardName, File tempImage, Path storageBase) throws Exception {
        CollectionFolder f = folderRepo.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("folder not found"));

        String imagePath = null;
        if (tempImage != null) {
            Files.createDirectories(storageBase);

            String baseName = tempImage.getName();
            String ext = baseName.contains(".") ? baseName.substring(baseName.lastIndexOf('.')) : ".png";
            String safeName = "card_" + System.currentTimeMillis() + "_" + Math.abs(baseName.hashCode()) + ext;

            Path target = storageBase.resolve(safeName);
            Files.move(tempImage.toPath(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // URL pública
            imagePath = toPublicImageUrl(target);
        }

        CardItem item = new CardItem();
        item.setFolderId(f.getId());
        item.setUserId(userId);
        item.setCardName(cardName);
        item.setPokemonName(dict.bestMatch(cardName).orElse(cardName));
        item.setSource(tempImage != null ? "manual+image" : "manual");
        item.setImagePath(imagePath); // pode ser null se não tiver imagem
        item.setCreatedAt(Instant.now());
        return itemRepo.save(item);
    }

    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        CardItem it = itemRepo.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("card not found"));
        if (!Objects.equals(it.getUserId(), userId)) {
            throw new IllegalArgumentException("card not found");
        }
        itemRepo.deleteById(cardId);
    }

    // dentro de CollectionService

    @Transactional
    public CardItem renameCard(Long userId, Long cardId, String newName) {
        CardItem it = itemRepo.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("card not found"));

        if (!Objects.equals(it.getUserId(), userId)) {
            throw new IllegalArgumentException("card not found");
        }

        it.setCardName(newName);

        // tenta normalizar para um nome de Pokémon conhecido
        String normalized = dict.bestMatchLoose(newName)
                .orElse(newName);
        it.setPokemonName(normalized);

        return itemRepo.save(it);
    }


    /* ==================== Helpers ==================== */

    // Converte caminho físico tipo:
    //   .../backend/auth/data/users/1/images/card_123.png
    // em URL pública:
    //   /files/users/1/images/card_123.png
    private String toPublicImageUrl(Path target) {
        Path dataRoot = Paths.get("data").toAbsolutePath().normalize();
        Path absoluteTarget = target.toAbsolutePath().normalize();

        try {
            Path relative = dataRoot.relativize(absoluteTarget);
            String rel = relative.toString().replace(File.separatorChar, '/');
            return "/files/" + rel;
        } catch (IllegalArgumentException e) {
            // fallback se não estiver dentro de "data"
            String abs = absoluteTarget.toString().replace(File.separatorChar, '/');
            return "/files/" + abs;
        }
    }

    // Fallback baseado em OCR "bruto"
    private String guessCardName(String raw) {
        if (raw == null || raw.isBlank()) return "Unknown";

        String cleaned = raw
                .replaceAll("[^\\p{L}\\p{Nd} \\-\\n]", " ")
                .toLowerCase(Locale.ROOT);

        String[] lines = cleaned.split("\\R+");

        // 1) tenta linha a linha usando a versão "solta" do dicionário
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() < 2) continue;

            var m = dict.bestMatchLoose(trimmed); // <<< trocamos aqui
            if (m.isPresent()) return m.get();
        }

        // 2) ainda assim, tenta token por token, também com bestMatchLoose
        Pattern split = Pattern.compile("[\\s\\-]+");
        for (String line : lines) {
            String[] toks = split.split(line.trim());
            for (String t : toks) {
                if (t.length() < 2) continue;
                var m = dict.bestMatchLoose(t);    // <<< e aqui
                if (m.isPresent()) return m.get();
            }
        }

        return "Unknown";
    }

}
