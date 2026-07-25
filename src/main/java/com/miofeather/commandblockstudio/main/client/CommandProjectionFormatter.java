package com.miofeather.commandblockstudio.main.client;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

final class CommandProjectionFormatter {
    private CommandProjectionFormatter() {
    }

    static List<String> format(String command, Font font, int wrapWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        int lastWordStart = -1;
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean escaped = false;

        for (int index = 0; index < command.length(); index++) {
            char character = command.charAt(index);
            if (font.width(current.toString()) > wrapWidth) {
                int split = lastWordStart >= 0 ? lastWordStart : current.length();
                submit(lines, current.substring(0, split), depth);
                current.delete(0, Math.min(split, current.length()));
                lastWordStart = -1;
            }

            if ((singleQuoted || doubleQuoted) && character == '\\') {
                escaped = !escaped;
                current.append(character);
                continue;
            }
            if (!CommandBlockStudio.FORMAT_STRINGS && !doubleQuoted && character == '\'' && !escaped) {
                singleQuoted = !singleQuoted;
                current.append(character);
                continue;
            }
            if (!CommandBlockStudio.FORMAT_STRINGS && !singleQuoted && character == '"' && !escaped) {
                doubleQuoted = !doubleQuoted;
                current.append(character);
                continue;
            }
            if (singleQuoted || doubleQuoted) {
                escaped = false;
                current.append(character);
                continue;
            }
            escaped = false;

            switch (character) {
                case '{', '[' -> {
                    if (CommandBlockStudio.NEWLINE_PRE_OPEN_BRACKET) {
                        submit(lines, current.toString(), depth);
                        current.setLength(0);
                    }
                    current.append(character);
                    if (CommandBlockStudio.NEWLINE_POST_OPEN_BRACKET) {
                        index = appendFollowingSpaces(command, index, current);
                        submit(lines, current.toString(), depth);
                        current.setLength(0);
                        depth++;
                    }
                    lastWordStart = -1;
                }
                case '}', ']' -> {
                    boolean depthChanged = false;
                    if (CommandBlockStudio.NEWLINE_PRE_CLOSE_BRACKET) {
                        submit(lines, current.toString(), depth);
                        current.setLength(0);
                        depth = Math.max(0, depth - 1);
                        depthChanged = true;
                    }
                    current.append(character);
                    if (CommandBlockStudio.NEWLINE_POST_CLOSE_BRACKET) {
                        index = appendFollowingSpaces(command, index, current);
                        submit(lines, current.toString(), depth);
                        current.setLength(0);
                        if (!depthChanged) {
                            depth = Math.max(0, depth - 1);
                        }
                    }
                    lastWordStart = -1;
                }
                case ',' -> {
                    current.append(character);
                    lastWordStart = current.length();
                    index = appendFollowingSpaces(command, index, current);
                    if (CommandBlockStudio.NEWLINE_POST_COMMA) {
                        submit(lines, current.toString(), depth);
                        current.setLength(0);
                        lastWordStart = -1;
                    }
                }
                case '\n', '\r' -> {
                    submit(lines, current.toString(), depth);
                    current.setLength(0);
                    lastWordStart = -1;
                }
                case ' ' -> {
                    lastWordStart = current.length();
                    current.append(character);
                }
                default -> current.append(character);
            }
        }
        submit(lines, current.toString(), depth);
        return lines.isEmpty() ? List.of("") : List.copyOf(lines);
    }

    static List<String> formatAndWrap(String command, Font font, int wrapWidth) {
        List<String> wrapped = new ArrayList<>();
        for (String line : format(command, font, wrapWidth)) {
            appendWrappedLine(wrapped, line, font, wrapWidth);
        }
        return wrapped.isEmpty() ? List.of("") : List.copyOf(wrapped);
    }

    static List<String> wrapPlain(String text, Font font, int wrapWidth) {
        List<String> wrapped = new ArrayList<>();
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalized.split("\n", -1)) {
            appendWrappedLine(wrapped, line, font, wrapWidth);
        }
        return wrapped.isEmpty() ? List.of("") : List.copyOf(wrapped);
    }

    private static void appendWrappedLine(List<String> wrapped, String line, Font font, int wrapWidth) {
        if (line.isEmpty()) {
            wrapped.add("");
            return;
        }
        List<FormattedText> split = font.getSplitter().splitLines(line, Math.max(1, wrapWidth), Style.EMPTY);
        if (split.isEmpty()) {
            wrapped.add(line);
            return;
        }
        split.stream().map(FormattedText::getString).forEach(wrapped::add);
    }

    private static int appendFollowingSpaces(String command, int index, StringBuilder current) {
        int next = index + 1;
        while (next < command.length() && command.charAt(next) == ' ') {
            current.append(' ');
            next++;
        }
        return next - 1;
    }

    private static void submit(List<String> lines, String line, int depth) {
        if (line.isBlank() && CommandBlockStudio.AVOID_DOUBLE_NEWLINE) {
            return;
        }
        String indent = String.valueOf(CommandBlockStudio.INDENTATION_CHAR)
                .repeat(Math.max(0, depth * CommandBlockStudio.INDENTATION_FACTOR));
        lines.add(indent + line);
    }
}
