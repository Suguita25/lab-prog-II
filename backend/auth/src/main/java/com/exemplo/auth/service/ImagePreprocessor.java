package com.exemplo.auth.service;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;

import java.io.File;
import java.io.IOException;

/**
 * Pré-processador de imagem para OCR das cartas:
 * recorta a faixa do título e aplica realce de contraste + binarização.
 */
public class ImagePreprocessor {

    // Fáceis de ajustar depois, se precisar
    private static final int TARGET_WIDTH = 900;     // largura mínima desejada
    private static final double TITLE_TOP_FRAC = 0.03;  // início da faixa (3% da altura)
    private static final double TITLE_HEIGHT_FRAC = 0.13; // altura da faixa (13% da altura)
    private static final int MIN_BAND_HEIGHT = 40;   // altura mínima em px

    /**
     * Lê a imagem, normaliza, recorta a faixa superior onde costuma ficar o título da carta,
     * faz binarização e salva um PNG temporário pronto para OCR.
     */
    public static File cropTitleBandForOcr(File input) {
        Mat src = opencv_imgcodecs.imread(input.getAbsolutePath(), opencv_imgcodecs.IMREAD_COLOR);
        if (src == null || src.empty()) {
            throw new IllegalArgumentException("Imagem vazia ou inválida: " + input);
        }

        Mat resized = new Mat();
        Mat band = new Mat();
        Mat gray = new Mat();
        Mat clahed = new Mat();
        Mat denoise = new Mat();
        Mat bin = new Mat();
        Mat kernel = new Mat();
        Mat morph = new Mat();

        try {
            // 1) Redimensiona para largura mínima (apenas se for pequena)
            int srcW = src.cols();
            int srcH = src.rows();

            double scale = srcW < TARGET_WIDTH
                    ? (TARGET_WIDTH / (double) srcW)
                    : 1.0;

            int newW = (int) Math.round(srcW * scale);
            int newH = (int) Math.round(srcH * scale);

            opencv_imgproc.resize(src, resized, new Size(newW, newH));

            // 2) Recorte da “faixa do título”
            int yOffset = (int) Math.round(newH * TITLE_TOP_FRAC);
            int bandH   = (int) Math.round(newH * TITLE_HEIGHT_FRAC);

            if (bandH < MIN_BAND_HEIGHT) bandH = MIN_BAND_HEIGHT;
            if (yOffset + bandH > newH) {
                bandH = newH - yOffset;
            }
            if (bandH <= 0) {
                // fallback: pega um terço do topo
                yOffset = 0;
                bandH = Math.max(newH / 3, MIN_BAND_HEIGHT);
                if (bandH > newH) bandH = newH;
            }

            Rect roi = new Rect(0, yOffset, newW, bandH);
            band = new Mat(resized, roi).clone();

            // 3) Cinza
            opencv_imgproc.cvtColor(band, gray, opencv_imgproc.COLOR_BGR2GRAY);

            // 4) CLAHE para realçar contraste
            var claheObj = opencv_imgproc.createCLAHE(3.0, new Size(8, 8));
            claheObj.apply(gray, clahed);
            claheObj.close(); // libera o objeto nativo

            // 5) Suavização preservando bordas (menos ruído)
            opencv_imgproc.bilateralFilter(clahed, denoise, 7, 75, 75);

            // 6) Binarização adaptativa
            opencv_imgproc.adaptiveThreshold(
                    denoise, bin, 255,
                    opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    opencv_imgproc.THRESH_BINARY, 31, 5
            );

            // 7) Pequena morfologia para fechar falhas nos caracteres
            kernel = opencv_imgproc.getStructuringElement(
                    opencv_imgproc.MORPH_RECT,
                    new Size(2, 2)
            );
            opencv_imgproc.morphologyEx(bin, morph, opencv_imgproc.MORPH_CLOSE, kernel);

            // 8) Salva em arquivo temporário
            File out;
            try {
                out = File.createTempFile("title_", ".png");
            } catch (IOException e) {
                throw new RuntimeException("Falha ao criar arquivo temporário", e);
            }
            opencv_imgcodecs.imwrite(out.getAbsolutePath(), morph);
            return out;

        } finally {
            // Libera todos os Mats criados
            src.release();
            resized.release();
            band.release();
            gray.release();
            clahed.release();
            denoise.release();
            bin.release();
            kernel.release();
            morph.release();
        }
    }
}
