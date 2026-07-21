package com.miofeather.commandblockstudio.main.ui;

import java.util.Optional;

public final class TextSearchEngine {
    private TextSearchEngine() {
    }

    public static int countMatches(String source, String query, boolean matchCase) {
        if (source == null || query == null || query.isEmpty()) {
            return 0;
        }
        int count = 0;
        int from = 0;
        while (from <= source.length() - query.length()) {
            int match = indexOf(source, query, from, matchCase);
            if (match < 0) {
                break;
            }
            count++;
            from = match + query.length();
        }
        return count;
    }

    public static Optional<Match> findMatch(
            String source,
            String query,
            int selectionStart,
            int selectionEnd,
            boolean backwards,
            boolean matchCase
    ) {
        if (source == null || source.isEmpty() || query == null || query.isEmpty()) {
            return Optional.empty();
        }
        int start = clamp(Math.min(selectionStart, selectionEnd), 0, source.length());
        int end = clamp(Math.max(selectionStart, selectionEnd), 0, source.length());
        int match;
        if (backwards) {
            match = lastIndexOf(source, query, start - 1, matchCase);
            if (match < 0) {
                match = lastIndexOf(source, query, source.length() - query.length(), matchCase);
            }
        } else {
            match = indexOf(source, query, end, matchCase);
            if (match < 0) {
                match = indexOf(source, query, 0, matchCase);
            }
        }
        return match < 0 ? Optional.empty() : Optional.of(new Match(match, match + query.length()));
    }

    public static boolean selectionMatches(
            String source,
            int selectionStart,
            int selectionEnd,
            String query,
            boolean matchCase
    ) {
        if (source == null || query == null) {
            return false;
        }
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        return start >= 0
                && end <= source.length()
                && end - start == query.length()
                && source.regionMatches(!matchCase, start, query, 0, query.length());
    }

    public static ReplaceAllResult replaceAll(String source, String query, String replacement, boolean matchCase) {
        String original = source == null ? "" : source;
        if (query == null || query.isEmpty()) {
            return new ReplaceAllResult(original, 0);
        }
        String replacementText = replacement == null ? "" : replacement;
        StringBuilder result = new StringBuilder(original.length());
        int count = 0;
        int from = 0;
        while (from <= original.length() - query.length()) {
            int match = indexOf(original, query, from, matchCase);
            if (match < 0) {
                break;
            }
            result.append(original, from, match).append(replacementText);
            from = match + query.length();
            count++;
        }
        if (count == 0) {
            return new ReplaceAllResult(original, 0);
        }
        result.append(original, from, original.length());
        return new ReplaceAllResult(result.toString(), count);
    }

    private static int indexOf(String source, String query, int from, boolean matchCase) {
        int first = clamp(from, 0, source.length());
        int last = source.length() - query.length();
        for (int index = first; index <= last; index++) {
            if (source.regionMatches(!matchCase, index, query, 0, query.length())) {
                return index;
            }
        }
        return -1;
    }

    private static int lastIndexOf(String source, String query, int from, boolean matchCase) {
        int first = Math.min(from, source.length() - query.length());
        for (int index = first; index >= 0; index--) {
            if (source.regionMatches(!matchCase, index, query, 0, query.length())) {
                return index;
            }
        }
        return -1;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Match(int start, int end) {
    }

    public record ReplaceAllResult(String text, int count) {
    }
}
