package com.exemplo.auth.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dicionário simples de nomes de Pokémon/cartas com matching por similaridade.
 */
@Service
public class PokemonDictionary {

    // Mantém referência estática à última instância criada para compatibilidade com chamadas estáticas.
    private static volatile PokemonDictionary INSTANCE;

    private final Set<String> names;          // normalizados (sem acento), em lower-case
    private final List<String> originalNames; // como carregados (para retornar ao usuário)

    public PokemonDictionary() {
        // tenta carregar de classpath:pokemon.txt
        List<String> loaded = loadFromClasspath("pokemon.txt");
        if (loaded.isEmpty()) {
            // fallback mínimo (adicione mais se quiser)
            loaded = Arrays.asList(
                    "Pikachu","Raichu","Squirtle","Wartortle","Blastoise",
                    "Bulbasaur","Ivysaur","Venusaur",
                    "Charmander","Charmeleon","Charizard",
                    "Pidgey","Pidgeotto","Pidgeot",
                    "Metagross","Gardevoir","Arcanine","Greninja",
                    "Riolu","Lucario","Mew","Mewtwo",
                    "Timburr","Gurdurr","Conkeldurr",
                    "Aipom","Ambipom"
            );
        }
        this.originalNames = new ArrayList<>(loaded);
        this.names = loaded.stream()
                .map(PokemonDictionary::norm)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        INSTANCE = this; // registra instância para score() estático
    }

    /* ===================== API ===================== */

    /** Melhor correspondência para a frase inteira (ex.: "Squirtle"). */
    public Optional<String> bestMatch(String query) {
        if (query == null || query.isBlank()) return Optional.empty();
        String nq = norm(query);
        String best = null;
        double bestScore = 0.0;

        int i = 0;
        for (String n : names) {
            double s = similarity(nq, n);
            if (s > bestScore) {
                bestScore = s;
                best = originalNames.get(i);
            }
            i++;
        }
        // limiar de confiança — ajuste conforme sua preferência
        if (bestScore >= 0.78) return Optional.of(best);
        return Optional.empty();
    }

    /**
     * Versão “relaxada”: tenta por tokens (ex.: "GX Squirtle" -> "Squirtle").
     * Útil quando o OCR traz palavras extras.
     */
    public Optional<String> bestMatchLoose(String query) {
        if (query == null || query.isBlank()) return Optional.empty();

        // 1) primeiro tenta a frase completa
        var mFull = bestMatch(query);
        if (mFull.isPresent()) return mFull;

        // 2) depois token a token
        String[] toks = norm(query).split("\\s+");
        String best = null;
        double bestScore = 0.0;

        for (String t : toks) {
            if (t.length() < 2) continue;
            int idx = 0;
            for (String n : names) {
                double s = similarity(t, n);
                if (s > bestScore) {
                    bestScore = s;
                    best = originalNames.get(idx);
                }
                idx++;
            }
        }
        if (best != null && bestScore >= 0.78) return Optional.of(best);
        return Optional.empty();
    }

    /**
     * Score (0..100) de “parece um nome de Pokémon” para um texto.
     * Útil para escolher entre múltiplas hipóteses de OCR.
     */
    public int score(String text) {
        if (text == null || text.isBlank()) return 0;
        String nq = norm(text);
        double best = 0.0;
        for (String n : names) {
            best = Math.max(best, similarity(nq, n));
        }
        return (int)Math.round(best * 100.0);
    }

    /** Versão estática para manter compatibilidade com chamadas existentes. */
    public static int scoreStatic(String text) {
        PokemonDictionary ref = INSTANCE;
        if (ref != null) return ref.score(text);
        // fallback mínimo se ainda não inicializou
        String nq = norm(text);
        // pequena lista fallback
        String[] base = {"pikachu","squirtle","bulbasaur","charmander","metagross","gardevoir"};
        double best = 0.0;
        for (String n : base) best = Math.max(best, similarity(nq, n));
        return (int)Math.round(best * 100.0);
    }

    /* ===================== Helpers ===================== */

    private static List<String> loadFromClasspath(String path) {
        try {
            var res = new ClassPathResource(path);
            if (!res.exists()) return List.of();
            try (var in = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                return in.lines()
                        .map(String::trim)
                        .filter(s -> !s.isBlank() && !s.startsWith("#"))
                        .distinct()
                        .toList();
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String norm(String s) {
        String t = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "") // remove acentos
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return t;
    }

    // Similaridade baseada em Levenshtein (normalizada)
    private static double similarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        int d = levenshtein(a, b);
        int max = Math.max(a.length(), b.length());
        if (max == 0) return 0.0;
        return 1.0 - ((double) d / (double) max);
    }

    private static int levenshtein(String a, String b) {
        int n = a.length(), m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int cost = (ca == b.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[m];
    }
}
