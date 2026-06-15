package com.acooldog.toolbox.utils;

import android.icu.text.Transliterator;

import androidx.annotation.NonNull;

import java.util.Locale;

public final class SearchSortUtils {
    private static final Transliterator HAN_TRANSLITERATOR =
            Transliterator.getInstance("Han-Latin; Latin-ASCII");

    private SearchSortUtils() {
    }

    @NonNull
    public static String normalize(@NonNull String input) {
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        return trimmed.replaceAll("\\s+", " ");
    }

    @NonNull
    public static String transliterate(@NonNull String input) {
        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return "";
        }
        String latin;
        try {
            latin = HAN_TRANSLITERATOR.transliterate(normalized);
        } catch (Exception ignored) {
            latin = normalized;
        }
        return latin.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    @NonNull
    public static String buildInitials(@NonNull String input) {
        String transliterated = transliterate(input);
        if (transliterated.isEmpty()) {
            return "";
        }
        String[] parts = transliterated.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.append(part.charAt(0));
            }
        }
        return builder.toString();
    }

    public static boolean matches(@NonNull String query, @NonNull String target) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        String normalizedTarget = normalize(target);
        if (normalizedTarget.contains(normalizedQuery)) {
            return true;
        }
        String transliterated = transliterate(target);
        if (transliterated.contains(normalizedQuery)) {
            return true;
        }
        return buildInitials(target).contains(normalizedQuery.replace(" ", ""));
    }

    @NonNull
    public static String buildSortKey(@NonNull String input) {
        String initials = buildInitials(input);
        String transliterated = transliterate(input);
        String normalized = normalize(input);
        return (initials.isEmpty() ? "~" : initials)
                + "|"
                + (transliterated.isEmpty() ? normalized : transliterated)
                + "|"
                + normalized;
    }
}
