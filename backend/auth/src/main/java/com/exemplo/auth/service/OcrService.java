package com.exemplo.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

@Service
public class OcrService {

    private final String tesseractPath;
    private final String tessdataPath;
    private final String languages;

    public OcrService(
            @Value("${app.ocr.tesseractPath}") String tesseractPath,
            @Value("${app.ocr.datapath}") String tessdataPath,
            @Value("${app.ocr.lang:eng+por}") String languages
    ) {
        this.tesseractPath = tesseractPath;
        this.tessdataPath = tessdataPath;
        this.languages = languages;
    }

    /** OCR bruto (sem crop) – ainda utilizamos em algumas situações. */
    public String extractText(File image) {
        return runTesseract(image, languages, null);
    }

    /** Tenta extrair **apenas o nome da carta**. */
// OcrService.extractCardName(...)
public String extractCardName(File fullImage) {
    try {
        BufferedImage src = ImageIO.read(fullImage);
        if (src == null) return null;

        int w = src.getWidth();
        int h = src.getHeight();

        // Faixa do nome: top 18% com margem lateral
        int topH = Math.max(40, (int) (h * 0.18));
        int x = (int) (w * 0.06);
        int cw = Math.min((int) (w * 0.88), w - x);

        BufferedImage roi = src.getSubimage(x, 0, cw, topH);
        BufferedImage pre = preprocess(roi);

        // Experimente 2 PSMs (linha única e poucas linhas) e escolha o "melhor" por comprimento/letras
        List<String> candidates = new ArrayList<>();
        candidates.add(runTesseract(writeTemp(pre), languages, List.of("--psm", "7",
                "-c", "tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz' -",
                "-c", "preserve_interword_spaces=1")));
        candidates.add(runTesseract(writeTemp(pre), languages, List.of("--psm", "6",
                "-c", "tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz' -",
                "-c", "preserve_interword_spaces=1")));

        // Normaliza e ranqueia por "quantidade de letras" (heurística simples)
        String best = null;
        int bestScore = -1;
        for (String raw : candidates) {
            String cleaned = normalizeText(raw);
            if (cleaned == null || cleaned.isBlank()) continue;
            int letters = cleaned.replaceAll("[^A-Za-z]", "").length();
            if (letters > bestScore) {
                bestScore = letters;
                best = cleaned;
            }
        }

        if (best != null && best.length() >= 2) best = toTitleWord(best);
        return best;
    } catch (Exception e) {
        throw new RuntimeException("Falha no OCR (nome)", e);
    }
}



private String normalizeCandidate(String s) {
    s = normalizeText(s);
    // remove palavras comuns que às vezes “vazam” do layout
    s = s.replaceAll("\\b(STAGE|BASICO|BÁSICO|NIVEL|NÍVEL|HP|PS)\\b", " ").trim();
    // pega a “maior palavra” se veio mais de uma
    String best = "";
    for (String part : s.split("\\s+")) {
        if (part.length() > best.length()) best = part;
    }
    return best.trim();
}


    /* ---------------- helpers ---------------- */

    private BufferedImage preprocess(BufferedImage img) {
        // escala 2×
        int nw = img.getWidth() * 2;
        int nh = img.getHeight() * 2;
        BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, nw, nh, null);
        g.dispose();

        // binarização simples (threshold global)
        BufferedImage bin = new BufferedImage(nw, nh, BufferedImage.TYPE_BYTE_BINARY);
        Graphics g2 = bin.getGraphics();
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();

        return bin;
    }

    private File writeTemp(BufferedImage img) throws Exception {
        File tmp = File.createTempFile("ocr_name_", ".png");
        ImageIO.write(img, "png", tmp);
        return tmp;
    }

    private String normalizeText(String s) {
        if (s == null) return null;
        s = s.replaceAll("[\\r\\n]+", " ").trim();
        // remove acentos para facilitar matching com dicionário
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        // remove ruídos nas pontas
        s = s.replaceAll("^[^A-Za-z]+", "").replaceAll("[^A-Za-z]+$", "");
        return s;
    }

    private String toTitleWord(String s) {
        String[] parts = s.toLowerCase().split("\\s+");
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            b.append(Character.toUpperCase(p.charAt(0)))
             .append(p.length() > 1 ? p.substring(1) : "")
             .append(' ');
        }
        return b.toString().trim();
    }

    private String runTesseract(File image, String langs, List<String> extraArgs) {
    try {
        List<String> cmd = new ArrayList<>();
        cmd.add(tesseractPath);
        cmd.add(image.getAbsolutePath());
        cmd.add("stdout");
        cmd.add("-l"); cmd.add(langs);           // ex.: "eng+por"
        cmd.add("--dpi"); cmd.add("300");

        // >>> garanta o caminho do tessdata
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            cmd.add("--tessdata-dir");            // funciona bem no Windows
            cmd.add(tessdataPath);
        }

        if (extraArgs != null) cmd.addAll(extraArgs);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            // opcional, mas ajuda em algumas instalações
            String prefix = tessdataPath.endsWith("\\") || tessdataPath.endsWith("/") ?
                    tessdataPath.substring(0, tessdataPath.length()-1) : tessdataPath;
            pb.environment().put("TESSDATA_PREFIX", new File(prefix).getParent());
        }
        pb.redirectErrorStream(true);

        Process p = pb.start();
        try (InputStream in = p.getInputStream()) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            int code = p.waitFor();
            if (code != 0) throw new RuntimeException("Tesseract retornou código " + code + " – " + text);
            return text;
        }
    } catch (Exception e) {
        throw new RuntimeException("Falha no OCR", e);
    }
}

}
