package com.miofeather.commandblockstudio.main.ui;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EditorTokenSelection {
    private static final Pattern NUMBER_TOKEN = Pattern.compile(
            "(?<![A-Za-z0-9_.])[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?"
    );

    private EditorTokenSelection() {
    }

    static Optional<Range> at(String text, int clickedIndex) {
        int index = Math.max(0, Math.min(clickedIndex, text.length()));
        Optional<Range> quoted = quotedContentAt(text, index);
        if (quoted.isPresent()) {
            return quoted;
        }

        Matcher matcher = NUMBER_TOKEN.matcher(text);
        while (matcher.find()) {
            if (index >= matcher.start() && index <= matcher.end()) {
                return Optional.of(new Range(matcher.start(), matcher.end()));
            }
        }
        return Optional.empty();
    }

    private static Optional<Range> quotedContentAt(String text, int clickedIndex) {
        char quote = '\0';
        int openingQuote = -1;
        boolean escaped = false;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (quote == '\0') {
                if (character == '"' || character == '\'') {
                    quote = character;
                    openingQuote = index;
                    escaped = false;
                }
                continue;
            }

            if (escaped) {
                escaped = false;
                continue;
            }
            if (character == '\\') {
                escaped = true;
                continue;
            }
            if (character != quote) {
                continue;
            }

            if (clickedIndex >= openingQuote && clickedIndex <= index + 1) {
                return Optional.of(new Range(openingQuote + 1, index));
            }
            quote = '\0';
            openingQuote = -1;
        }

        if (quote != '\0' && clickedIndex >= openingQuote && clickedIndex <= text.length()) {
            return Optional.of(new Range(openingQuote + 1, text.length()));
        }
        return Optional.empty();
    }

    record Range(int start, int end) {
    }
}
