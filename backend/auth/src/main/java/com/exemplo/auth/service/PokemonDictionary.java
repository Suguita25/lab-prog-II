// service/PokemonDictionary.java
package com.exemplo.auth.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PokemonDictionary {
    private final Set<String> names;

    public PokemonDictionary() {
        try (var in = new BufferedReader(new InputStreamReader(
                new ClassPathResource("pokemon.txt").getInputStream(), StandardCharsets.UTF_8))) {
            names = in.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toCollection(() -> new TreeSet<>()));
        } catch (Exception e) {
            throw new RuntimeException("Falha ao carregar pokemon.txt", e);
        }
    }

    public Optional<String> bestMatch(String text) {
        var t = text.toLowerCase(Locale.ROOT);
        // procura match exato por palavra
        for (String p : names) {
            if (t.contains(p)) return Optional.of(capitalize(p));
        }
        // fallback: procura palavras OCR prováveis
        for (String token : t.split("[^a-z0-9]+")) {
            if (names.contains(token)) return Optional.of(capitalize(token));
        }
        return Optional.empty();
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase()+s.substring(1);
    }
}
