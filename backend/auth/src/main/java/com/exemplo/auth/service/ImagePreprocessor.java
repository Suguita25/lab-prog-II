package com.exemplo.auth.service;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImagePreprocessor {

    /**
     * Lê a imagem, normaliza, recorta a faixa superior onde costuma ficar o título da carta,
     * faz binarização e salva um PNG temporário pronto para OCR.
     */
    public static File cropTitleBandForOcr(File input) {
        Mat src = opencv_imgcodecs.imread(input.getAbsolutePath(), opencv_imgcodecs.IMREAD_COLOR);
        if (src == null || src.empty()) {
            throw new IllegalArgumentException("Imagem vazia: " + input);
        }

        try {
            // 1) Redimensiona para largura padrão
            int targetW = 900;
            double scale = targetW / (double) src.cols();
            int newW = targetW;
            int newH = (int) Math.round(src.rows() * scale);
            Mat resized = new Mat();
            opencv_imgproc.resize(src, resized, new org.bytedeco.opencv.opencv_core.Size(newW, newH));

            // 2) Recorte da “faixa do título”
            int yOffset = (int) Math.round(newH * 0.03);
            int bandH  = (int) Math.round(newH * 0.13);
            if (yOffset + bandH > newH) bandH = newH - yOffset;
            Rect roi = new Rect(0, yOffset, newW, bandH);
            Mat band = new Mat(resized, roi).clone();

            // 3) Cinza + CLAHE + filtro bilateral
            Mat gray = new Mat();
            opencv_imgproc.cvtColor(band, gray, opencv_imgproc.COLOR_BGR2GRAY);

            var clahe = opencv_imgproc.createCLAHE(3.0, new org.bytedeco.opencv.opencv_core.Size(8, 8));
            Mat clahed = new Mat();
            clahe.apply(gray, clahed);

            Mat denoise = new Mat();
            opencv_imgproc.bilateralFilter(clahed, denoise, 7, 75, 75);

            // 4) Binarização adaptativa + morfologia
            Mat bin = new Mat();
            opencv_imgproc.adaptiveThreshold(
                    denoise, bin, 255,
                    opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    opencv_imgproc.THRESH_BINARY, 31, 5
            );
            Mat kernel = opencv_imgproc.getStructuringElement(
                    opencv_imgproc.MORPH_RECT,
                    new org.bytedeco.opencv.opencv_core.Size(2, 2)
            );
            Mat morph = new Mat();
            opencv_imgproc.morphologyEx(bin, morph, opencv_imgproc.MORPH_CLOSE, kernel);

            // 5) Salva temporário em PNG (capturando IOException)
            File out;
            try {
                out = File.createTempFile("title_", ".png");
            } catch (IOException e) {
                throw new RuntimeException("Falha ao criar arquivo temporário", e);
            }
            opencv_imgcodecs.imwrite(out.getAbsolutePath(), morph);
            return out;

        } finally {
            // Libera recursos principais (os que criamos diretamente). 
            // Se você quiser, pode guardar refs e liberar um a um como antes.
            src.release();
        }
    }
}
