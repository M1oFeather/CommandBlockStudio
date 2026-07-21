package com.miofeather.commandblockstudio.main.insight;

import java.util.List;

public record CommandSyntaxHint(
        String ghostText,
        String insertionText,
        String summary,
        int replacementStart,
        List<String> suggestions
) {
}
