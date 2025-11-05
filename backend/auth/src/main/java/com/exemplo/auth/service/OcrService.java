package com.exemplo.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class OcrService {

    // Caminhos/idiomas lidos do application.yml ou dos defaults abaixo
    private final String tesseractPath; // ex.: C:/Program Files/Tesseract-OCR/tesseract.exe  (ou C:/Progra~1/...)
    private final String tessdataPath;  // ex.: C:/Program Files/Tesseract-OCR/tessdata
    private final String languages;     // ex.: "eng" ou "eng+por"

    public OcrService(
            // DEFAULTS: ajudam a subir mesmo se faltar no YAML
            @Value("${app.ocr.tesseractPath:C:/Progra~1/Tesseract-OCR/tesseract.exe}") String tesseractPath,
            @Value("${app.ocr.datapath:C:/Progra~1/Tesseract-OCR/tessdata}")           String tessdataPath,
            @Value("${app.ocr.lang:eng}")                                              String languages
    ) {
        this.tesseractPath = tesseractPath;
        this.tessdataPath  = tessdataPath;
        this.languages     = languages;

        System.out.println("[OCR] tesseractPath = " + this.tesseractPath);
        System.out.println("[OCR] tessdataPath  = " + this.tessdataPath);
        System.out.println("[OCR] languages     = " + this.languages);

        // Validações amigáveis logo no start
        File exe = new File(this.tesseractPath);
        if (!exe.exists()) {
            throw new IllegalStateException("Tesseract não encontrado em: " + this.tesseractPath +
                    ". Ajuste app.ocr.tesseractPath no application.yml.");
        }
        File data = new File(this.tessdataPath);
        if (!data.exists() || !data.isDirectory()) {
            throw new IllegalStateException("Pasta tessdata inválida: " + this.tessdataPath +
                    ". Ajuste app.ocr.datapath no application.yml.");
        }
    }

    public String extractText(File image) {
        try {
            if (image == null || !image.exists()) {
                throw new IllegalArgumentException("Imagem inexistente.");
            }

            List<String> cmd = new ArrayList<>();
            cmd.add(tesseractPath);                 // ProcessBuilder trata espaços no caminho
            cmd.add(image.getAbsolutePath());
            cmd.add("stdout");                      // saída no STDOUT
            cmd.add("-l");                          // idiomas
            cmd.add(languages);
            cmd.add("--dpi");                       // ajuda em imagens sem metadata
            cmd.add("300");

            ProcessBuilder pb = new ProcessBuilder(cmd);

            // Garante que o tesseract ache os .traineddata
            String prefix = tessdataPath.endsWith("\\") || tessdataPath.endsWith("/")
                    ? tessdataPath
                    : tessdataPath + File.separator;
            pb.environment().put("TESSDATA_PREFIX", prefix);

            pb.redirectErrorStream(true);

            Process p = pb.start();
            try (InputStream in = p.getInputStream()) {
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                int code = p.waitFor();
                if (code != 0) {
                    throw new RuntimeException("Tesseract retornou código " + code +
                            ". Verifique idiomas instalados (ex.: eng, por) em " + prefix);
                }
                return text;
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha no OCR", e);
        }
    }
}
