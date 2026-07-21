package com.miofeather.commandblockstudio.main.insight;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Tolerant, editor-only completion for arguments whose native codecs do not expose nested suggestions.
 * Final validation remains owned by Brigadier and Minecraft's component codecs.
 */
final class StructuredArgumentCompletion {
    private static final List<String> ROOT_COMPONENT_SUGGESTIONS = List.of(
            "{\"text\":\"\"}",
            "{\"translate\":\"\"}",
            "{\"selector\":\"@s\"}",
            "{\"score\":{\"name\":\"@s\",\"objective\":\"\"}}",
            "[\"\",{\"text\":\"\"}]"
    );
    private static final String ROOT_COMPONENT_TEMPLATE = "{\"text\":\"<text>\"}";

    private static final Map<String, FieldDoc> COMPONENT_FIELDS = componentFields();
    private static final Map<String, FieldDoc> CLICK_EVENT_FIELDS = clickEventFields();
    private static final Map<String, FieldDoc> HOVER_EVENT_FIELDS = hoverEventFields();
    private static final Map<String, FieldDoc> SCORE_FIELDS = scoreFields();
    private static final Map<String, FieldDoc> HOVER_CONTENT_FIELDS = hoverContentFields();
    private static final Map<String, ItemComponentDoc> ITEM_COMPONENTS = itemComponents();

    private StructuredArgumentCompletion() {
    }

    static Optional<CommandSyntaxHint> findHint(
            String command,
            int cursor,
            ParseResults<SharedSuggestionProvider> parse
    ) {
        if (parse == null || command.isBlank() || cursor < 0 || cursor > command.length()) {
            return Optional.empty();
        }
        Optional<ArgumentContext> context = findArgumentContext(command, cursor, parse);
        if (context.isEmpty()) {
            return Optional.empty();
        }
        return switch (context.get().kind()) {
            case TEXT_COMPONENT -> textComponentHint(command, cursor, context.get().start(), false);
            case ITEM -> itemComponentHint(command, cursor, context.get().start());
        };
    }

    static Optional<CommandInsight> describeCursor(
            String command,
            int cursor,
            ParseResults<SharedSuggestionProvider> parse
    ) {
        if (parse == null || command.isBlank() || cursor < 0 || cursor > command.length()) {
            return Optional.empty();
        }
        Optional<ArgumentContext> context = findArgumentContext(command, cursor, parse);
        if (context.isEmpty()) {
            return Optional.empty();
        }
        return switch (context.get().kind()) {
            case TEXT_COMPONENT -> describeTextCursor(command, cursor, context.get().start(), false);
            case ITEM -> describeItemCursor(command, cursor, context.get().start());
        };
    }

    static Optional<CommandInsight> describeSuggestion(
            String command,
            int cursor,
            String suggestion,
            ParseResults<SharedSuggestionProvider> parse
    ) {
        if (parse == null || suggestion == null || suggestion.isBlank()) {
            return Optional.empty();
        }
        Optional<ArgumentContext> context = findArgumentContext(command, cursor, parse);
        if (context.isEmpty()) {
            return Optional.empty();
        }
        if (context.get().kind() == ArgumentKind.TEXT_COMPONENT) {
            JsonPosition position = analyzeJson(command.substring(context.get().start(), cursor));
            String key = position.key();
            if (position.type() == JsonPositionType.KEY || key == null) {
                key = extractJsonKey(suggestion);
            }
            return componentField(position.kind(), key).map(FieldDoc::insight);
        }

        Optional<ItemPosition> item = analyzeItem(command, cursor, context.get().start());
        if (item.isEmpty()) {
            return Optional.empty();
        }
        String key = item.get().key();
        if (item.get().phase() == ItemPhase.KEY) {
            key = normalizeItemComponentKey(suggestion);
        }
        return key == null || key.isBlank() ? Optional.empty() : Optional.of(itemDoc(key).insight());
    }

    private static Optional<ArgumentContext> findArgumentContext(
            String command,
            int cursor,
            ParseResults<SharedSuggestionProvider> parse
    ) {
        ArgumentContext parsedBest = null;
        for (ParsedCommandNode<SharedSuggestionProvider> parsedNode : flattenNodes(parse.getContext())) {
            ArgumentKind kind = argumentKind(parsedNode.getNode());
            StringRange range = parsedNode.getRange();
            if (kind != null && cursor >= range.getStart() && cursor <= range.getEnd()) {
                if (parsedBest == null || range.getStart() >= parsedBest.start()) {
                    parsedBest = new ArgumentContext(kind, range.getStart());
                }
            }
        }
        if (parsedBest != null) {
            return Optional.of(parsedBest);
        }

        try {
            SuggestionContext<SharedSuggestionProvider> suggestionContext =
                    parse.getContext().findSuggestionContext(cursor);
            List<CommandNode<SharedSuggestionProvider>> children = new ArrayList<>(suggestionContext.parent.getChildren());
            if (suggestionContext.parent.getRedirect() != null) {
                children.addAll(suggestionContext.parent.getRedirect().getChildren());
            }
            for (CommandNode<SharedSuggestionProvider> child : children) {
                ArgumentKind kind = argumentKind(child);
                if (kind != null) {
                    int start = skipWhitespace(command, Math.min(suggestionContext.startPos, cursor), cursor);
                    return Optional.of(new ArgumentContext(kind, start));
                }
            }
        } catch (IllegalArgumentException | IllegalStateException ignored) {
        }

        int previousEnd = 0;
        for (ParsedCommandNode<SharedSuggestionProvider> node : flattenNodes(parse.getContext())) {
            if (node.getRange().getEnd() <= cursor) {
                previousEnd = Math.max(previousEnd, node.getRange().getEnd());
            }
        }
        int estimatedStart = skipWhitespace(command, previousEnd, cursor);
        for (Map.Entry<CommandNode<SharedSuggestionProvider>, com.mojang.brigadier.exceptions.CommandSyntaxException> failure
                : parse.getExceptions().entrySet()) {
            ArgumentKind kind = argumentKind(failure.getKey());
            int failureCursor = failure.getValue().getCursor();
            if (kind != null && estimatedStart <= cursor
                    && (failureCursor < 0 || failureCursor >= estimatedStart && failureCursor <= cursor)) {
                return Optional.of(new ArgumentContext(kind, estimatedStart));
            }
        }
        return Optional.empty();
    }

    private static ArgumentKind argumentKind(CommandNode<SharedSuggestionProvider> node) {
        if (!(node instanceof ArgumentCommandNode<?, ?> argumentNode)) {
            return null;
        }
        return switch (argumentNode.getType().getClass().getSimpleName()) {
            case "ComponentArgument" -> ArgumentKind.TEXT_COMPONENT;
            case "ItemArgument" -> ArgumentKind.ITEM;
            default -> null;
        };
    }

    private static Optional<CommandSyntaxHint> textComponentHint(
            String command,
            int cursor,
            int componentStart,
            boolean embedded
    ) {
        if (componentStart < 0 || componentStart > cursor) {
            return Optional.empty();
        }
        JsonPosition position = analyzeJson(command.substring(componentStart, cursor));
        String prefixLabel = embedded
                ? localized("物品组件中的文本 Component。", "Text Component embedded in an item component. ")
                : localized("文本 Component。", "Text Component. ");

        return switch (position.type()) {
            case ROOT -> Optional.of(hint(
                    command,
                    cursor,
                    componentStart,
                    ROOT_COMPONENT_SUGGESTIONS,
                    ROOT_COMPONENT_TEMPLATE,
                    prefixLabel + localized("可使用纯字符串、对象或组件数组。", "Use a string, object, or component array.")
            ));
            case KEY -> buildJsonKeyHint(command, cursor, componentStart, position, prefixLabel);
            case COLON -> componentField(position.kind(), position.key()).map(field -> hint(
                    command,
                    cursor,
                    cursor,
                    prepend(":", field.values()),
                    ":" + field.template(),
                    prefixLabel + field.summary()
            ));
            case VALUE -> buildJsonValueHint(command, cursor, componentStart, position, prefixLabel);
            case SEPARATOR -> Optional.of(hint(
                    command,
                    cursor,
                    cursor,
                    position.container() == JsonContainer.OBJECT ? List.of(",", "}") : List.of(",", "]"),
                    position.container() == JsonContainer.OBJECT ? "}" : "]",
                    prefixLabel + localized("当前值已完成；可继续添加字段或关闭容器。", "The value is complete; add another entry or close the container.")
            ));
            case COMPLETE -> Optional.empty();
        };
    }

    private static Optional<CommandSyntaxHint> buildJsonKeyHint(
            String command,
            int cursor,
            int componentStart,
            JsonPosition position,
            String prefixLabel
    ) {
        Map<String, FieldDoc> fields = fieldsFor(position.kind());
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        PartialToken partial = position.partial();
        List<FieldDoc> available = fields.values().stream()
                .filter(field -> !position.usedKeys().contains(field.key()))
                .filter(field -> partial == null || field.key().startsWith(partial.text().toLowerCase(Locale.ROOT)))
                .toList();
        if (available.isEmpty()) {
            return Optional.empty();
        }

        int replacementStart;
        List<String> suggestions;
        String template;
        if (partial != null && partial.quoted()) {
            replacementStart = componentStart + partial.start() + 1;
            suggestions = available.stream().map(FieldDoc::key).toList();
            template = available.getFirst().key();
        } else if (partial != null) {
            replacementStart = componentStart + partial.start();
            suggestions = available.stream().map(FieldDoc::key).toList();
            template = available.getFirst().key();
        } else {
            replacementStart = cursor;
            suggestions = available.stream().map(FieldDoc::keySuggestion).toList();
            template = available.getFirst().keyTemplate();
        }
        return Optional.of(hint(
                command,
                cursor,
                replacementStart,
                suggestions,
                template,
                prefixLabel + localized("选择 Component 字段；候选会同时填入该字段的起始值。", "Choose a Component field; candidates include a starter value.")
        ));
    }

    private static Optional<CommandSyntaxHint> buildJsonValueHint(
            String command,
            int cursor,
            int componentStart,
            JsonPosition position,
            String prefixLabel
    ) {
        Optional<FieldDoc> field = componentField(position.kind(), position.key());
        if (field.isEmpty()) {
            return Optional.empty();
        }
        PartialToken partial = position.partial();
        int replacementStart = cursor;
        List<String> values = field.get().values();
        String template = field.get().template();
        if (partial != null && partial.quoted()) {
            replacementStart = componentStart + partial.start() + 1;
            String typed = partial.text();
            values = values.stream()
                    .filter(StructuredArgumentCompletion::isJsonString)
                    .map(StructuredArgumentCompletion::unquoteJsonString)
                    .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(typed.toLowerCase(Locale.ROOT)))
                    .toList();
            template = isJsonString(template) ? unquoteJsonString(template) : template;
        } else if (partial != null) {
            replacementStart = componentStart + partial.start();
            String typed = partial.text().toLowerCase(Locale.ROOT);
            values = values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
        }
        if (values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(hint(
                command,
                cursor,
                replacementStart,
                values,
                template,
                prefixLabel + field.get().summary()
        ));
    }

    private static Optional<CommandInsight> describeTextCursor(
            String command,
            int cursor,
            int componentStart,
            boolean embedded
    ) {
        if (componentStart < 0 || componentStart > cursor) {
            return Optional.empty();
        }
        JsonPosition position = analyzeJson(command.substring(componentStart, cursor));
        String key = position.key();
        if (position.type() == JsonPositionType.KEY && position.partial() != null) {
            String prefix = position.partial().text().toLowerCase(Locale.ROOT);
            key = fieldsFor(position.kind()).keySet().stream()
                    .filter(candidate -> candidate.startsWith(prefix))
                    .findFirst()
                    .orElse(key);
        }
        Optional<FieldDoc> field = componentField(position.kind(), key);
        if (field.isPresent()) {
            return Optional.of(field.get().insight());
        }
        String title = embedded ? "Item Component -> Text Component" : "Text Component";
        String summary = localized(
                "Minecraft 富文本结构；支持内容、样式、交互事件、选择器、计分板和 NBT 数据源。",
                "Minecraft rich text supporting content, styling, interaction events, selectors, scores, and NBT sources."
        );
        return Optional.of(new CommandInsight(title, summary, ROOT_COMPONENT_SUGGESTIONS, true));
    }

    private static Optional<CommandSyntaxHint> itemComponentHint(String command, int cursor, int itemStart) {
        Optional<ItemPosition> analyzed = analyzeItem(command, cursor, itemStart);
        if (analyzed.isEmpty()) {
            return Optional.empty();
        }
        ItemPosition position = analyzed.get();
        if (position.embeddedComponentStart() >= 0) {
            return textComponentHint(command, cursor, position.embeddedComponentStart(), true);
        }
        if (position.phase() == ItemPhase.KEY) {
            List<String> componentIds = itemComponentIds().stream()
                    .filter(id -> !position.usedKeys().contains(normalizeItemComponentKey(id)))
                    .filter(id -> normalizeItemComponentKey(id).startsWith(position.prefix().toLowerCase(Locale.ROOT))
                            || id.startsWith(position.prefix().toLowerCase(Locale.ROOT)))
                    .map(id -> position.removed() ? id : id + "=")
                    .toList();
            if (componentIds.isEmpty()) {
                return Optional.empty();
            }
            String firstKey = normalizeItemComponentKey(componentIds.getFirst());
            ItemComponentDoc firstDoc = itemDoc(firstKey);
            String template = position.removed()
                    ? componentIds.getFirst()
                    : itemComponentId(firstKey) + "=" + firstDoc.template();
            return Optional.of(hint(
                    command,
                    cursor,
                    position.replacementStart(),
                    componentIds,
                    template,
                    localized(
                            "物品数据组件键来自当前 1.21.1 DATA_COMPONENT_TYPE 注册表；!key 表示移除组件。",
                            "Item data-component keys come from the active 1.21.1 DATA_COMPONENT_TYPE registry; !key removes a component."
                    )
            ));
        }

        ItemComponentDoc doc = itemDoc(position.key());
        if (position.phase() == ItemPhase.VALUE && !doc.values().isEmpty()) {
            List<String> values = doc.values().stream()
                    .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(position.prefix().toLowerCase(Locale.ROOT)))
                    .toList();
            if (values.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(hint(
                    command,
                    cursor,
                    position.replacementStart(),
                    values,
                    doc.template(),
                    doc.summary()
            ));
        }
        return Optional.empty();
    }

    private static Optional<CommandInsight> describeItemCursor(String command, int cursor, int itemStart) {
        Optional<ItemPosition> analyzed = analyzeItem(command, cursor, itemStart);
        if (analyzed.isEmpty()) {
            return Optional.of(new CommandInsight(
                    "Item Component",
                    localized(
                            "物品 id 后可使用 [component=value]；组件键由当前注册表提供，值由对应原生 Codec 校验。",
                            "Use [component=value] after an item id. Keys come from the active registry and values are validated by each native codec."
                    ),
                    List.of("minecraft:diamond[minecraft:custom_name='{\"text\":\"Blade\"}']"),
                    true
            ));
        }
        ItemPosition position = analyzed.get();
        if (position.embeddedComponentStart() >= 0) {
            return describeTextCursor(command, cursor, position.embeddedComponentStart(), true);
        }
        if (position.key() != null && !position.key().isBlank()) {
            return Optional.of(itemDoc(position.key()).insight());
        }
        return Optional.of(new CommandInsight(
                "Item Component",
                localized(
                        "选择一个数据组件键；输入 ! 可移除组件，输入 = 后会显示该组件的值模板。",
                        "Choose a data-component key. Prefix it with ! to remove it, or type = to receive a value template."
                ),
                List.of("minecraft:stone[minecraft:custom_data={key:1}]"),
                true
        ));
    }

    private static Optional<ItemPosition> analyzeItem(String command, int cursor, int itemStart) {
        if (itemStart < 0 || itemStart > cursor) {
            return Optional.empty();
        }
        String prefix = command.substring(itemStart, cursor);
        int componentOpen = findTopLevelComponentOpen(prefix);
        if (componentOpen < 0) {
            return Optional.empty();
        }

        int squareDepth = 1;
        int curlyDepth = 0;
        int parenDepth = 0;
        int segmentStart = componentOpen + 1;
        int assignment = -1;
        char quote = 0;
        boolean escaped = false;
        Set<String> usedKeys = new HashSet<>();
        for (int i = componentOpen + 1; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                continue;
            }
            if (c == '[') squareDepth++;
            else if (c == ']') squareDepth--;
            else if (c == '{') curlyDepth++;
            else if (c == '}') curlyDepth--;
            else if (c == '(') parenDepth++;
            else if (c == ')') parenDepth--;
            else if (squareDepth == 1 && curlyDepth == 0 && parenDepth == 0 && c == '=') {
                if (assignment < segmentStart) assignment = i;
            } else if (squareDepth == 1 && curlyDepth == 0 && parenDepth == 0 && c == ',') {
                collectItemKey(prefix.substring(segmentStart, i)).ifPresent(usedKeys::add);
                segmentStart = i + 1;
                assignment = -1;
            }
            if (squareDepth <= 0) {
                return Optional.empty();
            }
        }

        int trimmedStart = skipWhitespace(prefix, segmentStart, prefix.length());
        boolean removed = trimmedStart < prefix.length() && prefix.charAt(trimmedStart) == '!';
        int keyStart = removed ? trimmedStart + 1 : trimmedStart;
        if (assignment < keyStart) {
            String keyPrefix = prefix.substring(keyStart).strip();
            return Optional.of(new ItemPosition(
                    ItemPhase.KEY,
                    null,
                    keyPrefix,
                    itemStart + keyStart,
                    removed,
                    Set.copyOf(usedKeys),
                    -1
            ));
        }

        String key = normalizeItemComponentKey(prefix.substring(keyStart, assignment).strip());
        usedKeys.add(key);
        int valueStart = skipWhitespace(prefix, assignment + 1, prefix.length());
        String valuePrefix = prefix.substring(valueStart);
        int embeddedStart = embeddedTextComponentStart(valuePrefix);
        if (embeddedStart >= 0 && supportsEmbeddedTextComponent(key)) {
            return Optional.of(new ItemPosition(
                    ItemPhase.VALUE,
                    key,
                    valuePrefix,
                    itemStart + valueStart,
                    false,
                    Set.copyOf(usedKeys),
                    itemStart + valueStart + embeddedStart
            ));
        }
        int simpleStart = valueStart;
        while (simpleStart < prefix.length() && Character.isWhitespace(prefix.charAt(simpleStart))) simpleStart++;
        return Optional.of(new ItemPosition(
                ItemPhase.VALUE,
                key,
                prefix.substring(simpleStart),
                itemStart + simpleStart,
                false,
                Set.copyOf(usedKeys),
                -1
        ));
    }

    private static int embeddedTextComponentStart(String valuePrefix) {
        char quote = 0;
        int quoteStart = -1;
        boolean escaped = false;
        for (int i = 0; i < valuePrefix.length(); i++) {
            char c = valuePrefix.charAt(i);
            if (quote == 0) {
                if (c == '\'') {
                    quote = c;
                    quoteStart = i + 1;
                }
            } else if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == quote) {
                quote = 0;
                quoteStart = -1;
            }
        }
        if (quote == '\'' && quoteStart >= 0) {
            String embedded = valuePrefix.substring(quoteStart).stripLeading();
            if (embedded.startsWith("{") || embedded.startsWith("[") || embedded.startsWith("\"")) {
                return quoteStart + (valuePrefix.substring(quoteStart).length() - valuePrefix.substring(quoteStart).stripLeading().length());
            }
        }
        return -1;
    }

    private static boolean supportsEmbeddedTextComponent(String key) {
        return Set.of("custom_name", "item_name", "lore", "written_book_content", "writable_book_content").contains(key);
    }

    private static int findTopLevelComponentOpen(String value) {
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quote != 0) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '[') {
                return i;
            }
        }
        return -1;
    }

    private static Optional<String> collectItemKey(String segment) {
        int assignment = segment.indexOf('=');
        String key = assignment >= 0 ? segment.substring(0, assignment) : segment;
        key = normalizeItemComponentKey(key.strip().replaceFirst("^!", ""));
        return key.isBlank() ? Optional.empty() : Optional.of(key);
    }

    private static List<String> itemComponentIds() {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.entrySet().stream()
                .filter(entry -> {
                    DataComponentType<?> type = entry.getValue();
                    return type != null && !type.isTransient();
                })
                .map(entry -> entry.getKey().location().toString())
                .sorted()
                .toList();
    }

    private static String itemComponentId(String normalizedKey) {
        return itemComponentIds().stream()
                .filter(id -> normalizeItemComponentKey(id).equals(normalizedKey))
                .findFirst()
                .orElse("minecraft:" + normalizedKey);
    }

    private static ItemComponentDoc itemDoc(String rawKey) {
        String key = normalizeItemComponentKey(rawKey);
        ItemComponentDoc documented = ITEM_COMPONENTS.get(key);
        if (documented != null) {
            return documented;
        }
        return new ItemComponentDoc(
                key,
                "Registered item data component. Its value is decoded by the component's native codec.",
                "已注册的物品数据组件；其值由该组件的原生 Codec 解码。",
                List.of(),
                "<value>",
                List.of(itemComponentId(key) + "=<value>")
        );
    }

    private static String normalizeItemComponentKey(String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.endsWith("=")) normalized = normalized.substring(0, normalized.length() - 1);
        if (normalized.startsWith("!")) normalized = normalized.substring(1);
        int namespace = normalized.indexOf(':');
        return (namespace >= 0 ? normalized.substring(namespace + 1) : normalized).toLowerCase(Locale.ROOT);
    }

    private static JsonPosition analyzeJson(String prefix) {
        List<JsonToken> tokens = tokenizeJson(prefix);
        if (tokens.isEmpty()) {
            return JsonPosition.root();
        }
        Deque<JsonFrame> stack = new ArrayDeque<>();

        for (int i = 0; i < tokens.size(); i++) {
            JsonToken token = tokens.get(i);
            boolean last = i == tokens.size() - 1;
            if (stack.isEmpty()) {
                if (token.type() == JsonTokenType.LBRACE) {
                    stack.push(JsonFrame.object(JsonKind.COMPONENT));
                    continue;
                }
                if (token.type() == JsonTokenType.LBRACKET) {
                    stack.push(JsonFrame.array(JsonKind.COMPONENT_ARRAY));
                    continue;
                }
                if (!token.closed() || last) {
                    return JsonPosition.complete();
                }
                continue;
            }

            JsonFrame frame = stack.peek();
            if (frame.container == JsonContainer.OBJECT) {
                if (frame.expect == JsonExpect.KEY) {
                    if (token.type() == JsonTokenType.RBRACE) {
                        stack.pop();
                    } else if (token.type() == JsonTokenType.STRING) {
                        if (!token.closed()) {
                            return JsonPosition.key(frame, PartialToken.quoted(token));
                        }
                        frame.key = token.text();
                        frame.usedKeys.add(frame.key.toLowerCase(Locale.ROOT));
                        frame.expect = JsonExpect.COLON;
                    } else if (last) {
                        return JsonPosition.key(frame, PartialToken.atom(token));
                    }
                } else if (frame.expect == JsonExpect.COLON) {
                    if (token.type() == JsonTokenType.COLON) {
                        frame.expect = JsonExpect.VALUE;
                    }
                } else if (frame.expect == JsonExpect.VALUE) {
                    if (token.type() == JsonTokenType.STRING && !token.closed()) {
                        return JsonPosition.value(frame, PartialToken.quoted(token));
                    }
                    if (token.type() == JsonTokenType.ATOM && last) {
                        return JsonPosition.value(frame, PartialToken.atom(token));
                    }
                    frame.expect = JsonExpect.SEPARATOR;
                    if (token.type() == JsonTokenType.LBRACE) {
                        stack.push(JsonFrame.object(childKind(frame.kind, frame.key)));
                    } else if (token.type() == JsonTokenType.LBRACKET) {
                        stack.push(JsonFrame.array(arrayKind(frame.kind, frame.key)));
                    }
                } else if (frame.expect == JsonExpect.SEPARATOR) {
                    if (token.type() == JsonTokenType.COMMA) {
                        frame.key = null;
                        frame.expect = JsonExpect.KEY;
                    } else if (token.type() == JsonTokenType.RBRACE) {
                        stack.pop();
                    }
                }
            } else {
                if (frame.expect == JsonExpect.VALUE) {
                    if (token.type() == JsonTokenType.RBRACKET) {
                        stack.pop();
                    } else if (token.type() == JsonTokenType.STRING && !token.closed()) {
                        return JsonPosition.value(frame, PartialToken.quoted(token));
                    } else if (token.type() == JsonTokenType.ATOM && last) {
                        return JsonPosition.value(frame, PartialToken.atom(token));
                    } else {
                        frame.expect = JsonExpect.SEPARATOR;
                        if (token.type() == JsonTokenType.LBRACE) {
                            stack.push(JsonFrame.object(frame.kind == JsonKind.COMPONENT_ARRAY ? JsonKind.COMPONENT : JsonKind.GENERIC));
                        } else if (token.type() == JsonTokenType.LBRACKET) {
                            stack.push(JsonFrame.array(frame.kind));
                        }
                    }
                } else if (frame.expect == JsonExpect.SEPARATOR) {
                    if (token.type() == JsonTokenType.COMMA) {
                        frame.expect = JsonExpect.VALUE;
                    } else if (token.type() == JsonTokenType.RBRACKET) {
                        stack.pop();
                    }
                }
            }
        }

        if (stack.isEmpty()) {
            return JsonPosition.complete();
        }
        JsonFrame frame = stack.peek();
        return switch (frame.expect) {
            case KEY -> JsonPosition.key(frame, null);
            case COLON -> JsonPosition.colon(frame);
            case VALUE -> JsonPosition.value(frame, null);
            case SEPARATOR -> JsonPosition.separator(frame);
        };
    }

    private static List<JsonToken> tokenizeJson(String value) {
        List<JsonToken> tokens = new ArrayList<>();
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            JsonTokenType punctuation = switch (c) {
                case '{' -> JsonTokenType.LBRACE;
                case '}' -> JsonTokenType.RBRACE;
                case '[' -> JsonTokenType.LBRACKET;
                case ']' -> JsonTokenType.RBRACKET;
                case ':' -> JsonTokenType.COLON;
                case ',' -> JsonTokenType.COMMA;
                default -> null;
            };
            if (punctuation != null) {
                tokens.add(new JsonToken(punctuation, String.valueOf(c), i, i + 1, true));
                i++;
                continue;
            }
            if (c == '"') {
                int start = i++;
                StringBuilder text = new StringBuilder();
                boolean escaped = false;
                boolean closed = false;
                while (i < value.length()) {
                    char current = value.charAt(i++);
                    if (escaped) {
                        text.append(current);
                        escaped = false;
                    } else if (current == '\\') {
                        escaped = true;
                    } else if (current == '"') {
                        closed = true;
                        break;
                    } else {
                        text.append(current);
                    }
                }
                tokens.add(new JsonToken(JsonTokenType.STRING, text.toString(), start, i, closed));
                continue;
            }
            int start = i;
            while (i < value.length() && !Character.isWhitespace(value.charAt(i))
                    && "{}[],:\"".indexOf(value.charAt(i)) < 0) {
                i++;
            }
            tokens.add(new JsonToken(JsonTokenType.ATOM, value.substring(start, i), start, i, false));
        }
        return tokens;
    }

    private static JsonKind childKind(JsonKind parent, String key) {
        if (parent == JsonKind.COMPONENT) {
            if ("clickEvent".equals(key)) return JsonKind.CLICK_EVENT;
            if ("hoverEvent".equals(key)) return JsonKind.HOVER_EVENT;
            if ("score".equals(key)) return JsonKind.SCORE;
            if ("separator".equals(key)) return JsonKind.COMPONENT;
        }
        if (parent == JsonKind.HOVER_EVENT && "contents".equals(key)) return JsonKind.HOVER_CONTENT;
        return JsonKind.GENERIC;
    }

    private static JsonKind arrayKind(JsonKind parent, String key) {
        if (parent == JsonKind.COMPONENT && ("extra".equals(key) || "with".equals(key))) {
            return JsonKind.COMPONENT_ARRAY;
        }
        return JsonKind.GENERIC;
    }

    private static Optional<FieldDoc> componentField(JsonKind kind, String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(fieldsFor(kind).get(key));
    }

    private static Map<String, FieldDoc> fieldsFor(JsonKind kind) {
        return switch (kind) {
            case COMPONENT -> COMPONENT_FIELDS;
            case CLICK_EVENT -> CLICK_EVENT_FIELDS;
            case HOVER_EVENT -> HOVER_EVENT_FIELDS;
            case SCORE -> SCORE_FIELDS;
            case HOVER_CONTENT -> HOVER_CONTENT_FIELDS;
            default -> Map.of();
        };
    }

    private static String extractJsonKey(String suggestion) {
        String value = suggestion.strip();
        if (value.startsWith("\"")) {
            int end = value.indexOf('"', 1);
            if (end > 1) return value.substring(1, end);
        }
        int colon = value.indexOf(':');
        return (colon >= 0 ? value.substring(0, colon) : value).replace("\"", "").strip();
    }

    private static CommandSyntaxHint hint(
            String command,
            int cursor,
            int replacementStart,
            List<String> suggestions,
            String template,
            String summary
    ) {
        int safeStart = Math.max(0, Math.min(replacementStart, cursor));
        String typed = command.substring(safeStart, cursor);
        String insertion = template.startsWith(typed) ? template.substring(typed.length()) : template;
        return new CommandSyntaxHint(insertion, insertion, summary, safeStart, List.copyOf(suggestions));
    }

    private static List<String> prepend(String prefix, List<String> values) {
        return values.stream().map(value -> prefix + value).toList();
    }

    private static boolean isJsonString(String value) {
        return value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"';
    }

    private static String unquoteJsonString(String value) {
        return isJsonString(value) ? value.substring(1, value.length() - 1) : value;
    }

    private static int skipWhitespace(String value, int start, int limit) {
        int index = Math.max(0, start);
        int maximum = Math.min(limit, value.length());
        while (index < maximum && Character.isWhitespace(value.charAt(index))) index++;
        return index;
    }

    private static List<ParsedCommandNode<SharedSuggestionProvider>> flattenNodes(
            CommandContextBuilder<SharedSuggestionProvider> context
    ) {
        List<ParsedCommandNode<SharedSuggestionProvider>> nodes = new ArrayList<>();
        CommandContextBuilder<SharedSuggestionProvider> current = context;
        while (current != null) {
            nodes.addAll(current.getNodes());
            current = current.getChild();
        }
        return nodes;
    }

    private static String localized(String chinese, String english) {
        return CommandBlockStudio.useChineseCommandInsight() ? chinese : english;
    }

    private static Map<String, FieldDoc> componentFields() {
        Map<String, FieldDoc> fields = new LinkedHashMap<>();
        add(fields, field("text", "Literal text.", "直接显示的文本。", List.of("\"\""), "\"<text>\"", "{\"text\":\"Hello\"}"));
        add(fields, field("translate", "Translation key resolved from the active language.", "按当前语言解析的翻译键。", List.of("\"\""), "\"<translation_key>\"", "{\"translate\":\"block.minecraft.stone\"}"));
        add(fields, field("fallback", "Fallback text used when a translation key is missing.", "翻译键不存在时使用的后备文本。", List.of("\"\""), "\"<fallback>\"", "{\"translate\":\"example.key\",\"fallback\":\"Example\"}"));
        add(fields, field("with", "Component arguments inserted into a translated string.", "插入翻译字符串占位符的组件参数。", List.of("[]", "[{\"text\":\"\"}]"), "[{\"text\":\"<argument>\"}]", "{\"translate\":\"chat.type.text\",\"with\":[\"Player\",\"Hello\"]}"));
        add(fields, field("keybind", "Displays the localized key assigned to a key binding.", "显示某个按键绑定当前对应的本地化按键名。", List.of("\"key.jump\"", "\"key.sneak\""), "\"<keybind>\"", "{\"keybind\":\"key.jump\"}"));
        add(fields, field("score", "Displays one scoreboard value using name and objective.", "通过名称和计分板目标显示一个分数。", List.of("{\"name\":\"@s\",\"objective\":\"\"}"), "{\"name\":\"@s\",\"objective\":\"<objective>\"}", "{\"score\":{\"name\":\"@s\",\"objective\":\"kills\"}}"));
        add(fields, field("selector", "Displays the names matched by an entity selector.", "显示实体选择器匹配到的名称。", List.of("\"@s\"", "\"@p\"", "\"@a\"", "\"@e\""), "\"@s\"", "{\"selector\":\"@a\"}"));
        add(fields, field("separator", "Component inserted between selector or NBT results.", "插入在选择器或 NBT 多个结果之间的组件。", List.of("{\"text\":\", \"}"), "{\"text\":\"<separator>\"}", "{\"selector\":\"@a\",\"separator\":{\"text\":\", \"}}"));
        add(fields, field("nbt", "NBT path whose values are displayed.", "要读取并显示的 NBT 路径。", List.of("\"\""), "\"<nbt_path>\"", "{\"nbt\":\"Health\",\"entity\":\"@s\"}"));
        add(fields, field("interpret", "When true, NBT strings are parsed as nested text Components.", "为 true 时把 NBT 字符串继续解析为文本 Component。", List.of("true", "false"), "true", "{\"nbt\":\"CustomName\",\"entity\":\"@s\",\"interpret\":true}"));
        add(fields, field("block", "Block position used as the NBT data source.", "作为 NBT 数据源的方块坐标。", List.of("\"~ ~ ~\""), "\"<x> <y> <z>\"", "{\"nbt\":\"Items\",\"block\":\"~ ~ ~\"}"));
        add(fields, field("entity", "Entity selector used as the NBT data source.", "作为 NBT 数据源的实体选择器。", List.of("\"@s\"", "\"@p\"", "\"@e[limit=1]\""), "\"@s\"", "{\"nbt\":\"Health\",\"entity\":\"@s\"}"));
        add(fields, field("storage", "Command storage id used as the NBT data source.", "作为 NBT 数据源的命令存储 id。", List.of("\"minecraft:example\""), "\"<namespace:path>\"", "{\"nbt\":\"value\",\"storage\":\"minecraft:example\"}"));
        add(fields, field("extra", "Additional sibling Components appended after this one.", "追加在当前组件之后的子组件列表。", List.of("[]", "[{\"text\":\"\"}]"), "[{\"text\":\"<text>\"}]", "{\"text\":\"A\",\"extra\":[{\"text\":\"B\"}]}"));
        add(fields, field("color", "Text color name or #RRGGBB value.", "文本颜色名称或 #RRGGBB 值。", jsonStrings("black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white", "#RRGGBB"), "\"<color>\"", "{\"text\":\"Warning\",\"color\":\"red\"}"));
        add(fields, field("font", "Font resource location used to render this Component.", "渲染当前组件时使用的字体资源位置。", List.of("\"minecraft:default\"", "\"minecraft:uniform\"", "\"minecraft:alt\""), "\"minecraft:default\"", "{\"text\":\"Text\",\"font\":\"minecraft:uniform\"}"));
        for (String style : List.of("bold", "italic", "underlined", "strikethrough", "obfuscated")) {
            add(fields, field(style, "Boolean text style flag.", "布尔文本样式开关。", List.of("true", "false"), "true", "{\"text\":\"Text\",\"" + style + "\":true}"));
        }
        add(fields, field("insertion", "Text inserted into chat when the user Shift-clicks the Component.", "用户 Shift 点击组件时插入聊天栏的文本。", List.of("\"\""), "\"<text>\"", "{\"text\":\"Insert\",\"insertion\":\"example\"}"));
        add(fields, field("clickEvent", "Action performed when the Component is clicked.", "点击组件时执行的动作。", List.of("{\"action\":\"run_command\",\"value\":\"/help\"}"), "{\"action\":\"run_command\",\"value\":\"/<command>\"}", "{\"text\":\"Run\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/help\"}}"));
        add(fields, field("hoverEvent", "Content displayed while the pointer hovers over the Component.", "鼠标悬停在组件上时显示的内容。", List.of("{\"action\":\"show_text\",\"contents\":{\"text\":\"\"}}"), "{\"action\":\"show_text\",\"contents\":{\"text\":\"<text>\"}}", "{\"text\":\"Info\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"Details\"}}"));
        add(fields, field("type", "Explicit Component content type. Usually optional because legacy field matching is supported.", "显式指定组件内容类型；通常可省略，因为原版支持按字段自动匹配。", jsonStrings("text", "translatable", "keybind", "score", "selector", "nbt"), "\"text\"", "{\"type\":\"text\",\"text\":\"Hello\"}"));
        return immutableOrderedMap(fields);
    }

    private static Map<String, FieldDoc> clickEventFields() {
        Map<String, FieldDoc> fields = new LinkedHashMap<>();
        add(fields, field("action", "Click action. open_file is unavailable from dedicated servers.", "点击动作；专用服务器不能下发 open_file。", jsonStrings("open_url", "run_command", "suggest_command", "change_page", "copy_to_clipboard"), "\"run_command\"", "{\"action\":\"run_command\",\"value\":\"/help\"}"));
        add(fields, field("value", "Payload interpreted by the selected click action.", "由所选点击动作解释的负载。", List.of("\"/help\"", "\"https://example.com\"", "\"text\""), "\"<value>\"", "{\"action\":\"suggest_command\",\"value\":\"/tell \"}"));
        return immutableOrderedMap(fields);
    }

    private static Map<String, FieldDoc> hoverEventFields() {
        Map<String, FieldDoc> fields = new LinkedHashMap<>();
        add(fields, field("action", "Hover payload type.", "悬停内容类型。", jsonStrings("show_text", "show_item", "show_entity"), "\"show_text\"", "{\"action\":\"show_text\",\"contents\":\"Info\"}"));
        add(fields, field("contents", "Typed hover payload: a Component, item stack, or entity description.", "带类型的悬停负载：文本组件、物品堆或实体描述。", List.of("{\"text\":\"\"}", "{\"id\":\"minecraft:stone\"}"), "{\"text\":\"<text>\"}", "{\"action\":\"show_item\",\"contents\":{\"id\":\"minecraft:diamond\"}}"));
        add(fields, field("value", "Legacy hover payload. Prefer contents in new commands.", "旧版悬停负载；新命令建议使用 contents。", List.of("{\"text\":\"\"}"), "{\"text\":\"<text>\"}", "{\"action\":\"show_text\",\"value\":\"Info\"}"));
        return immutableOrderedMap(fields);
    }

    private static Map<String, FieldDoc> scoreFields() {
        Map<String, FieldDoc> fields = new LinkedHashMap<>();
        add(fields, field("name", "Score holder name or a selector matching one entity.", "分数持有者名称，或只匹配一个实体的选择器。", List.of("\"@s\"", "\"@p\"", "\"*\""), "\"@s\"", "{\"name\":\"@s\",\"objective\":\"kills\"}"));
        add(fields, field("objective", "Scoreboard objective whose value is displayed.", "要显示数值的计分板目标。", List.of("\"\""), "\"<objective>\"", "{\"name\":\"@s\",\"objective\":\"kills\"}"));
        return immutableOrderedMap(fields);
    }

    private static Map<String, FieldDoc> hoverContentFields() {
        Map<String, FieldDoc> fields = new LinkedHashMap<>(COMPONENT_FIELDS);
        add(fields, field("id", "Item id used by show_item.", "show_item 使用的物品 id。", List.of("\"minecraft:stone\"", "\"minecraft:diamond\""), "\"minecraft:<item>\"", "{\"id\":\"minecraft:diamond\",\"count\":1}"));
        add(fields, field("count", "Item count used by show_item.", "show_item 使用的物品数量。", List.of("1", "64"), "1", "{\"id\":\"minecraft:stone\",\"count\":64}"));
        add(fields, field("components", "Data-component patch for a show_item stack.", "show_item 物品堆的数据组件补丁。", List.of("{}"), "{<component>:<value>}", "{\"id\":\"minecraft:diamond\",\"components\":{}}"));
        add(fields, field("type", "Entity type id used by show_entity.", "show_entity 使用的实体类型 id。", List.of("\"minecraft:pig\"", "\"minecraft:player\""), "\"minecraft:<entity_type>\"", "{\"type\":\"minecraft:pig\",\"id\":\"00000000-0000-0000-0000-000000000000\"}"));
        add(fields, field("name", "Optional Component name used by show_entity.", "show_entity 使用的可选实体名称组件。", List.of("{\"text\":\"\"}"), "{\"text\":\"<name>\"}", "{\"type\":\"minecraft:pig\",\"id\":\"00000000-0000-0000-0000-000000000000\",\"name\":\"Pig\"}"));
        return immutableOrderedMap(fields);
    }

    private static Map<String, ItemComponentDoc> itemComponents() {
        Map<String, ItemComponentDoc> docs = new LinkedHashMap<>();
        add(docs, item("custom_data", "Custom SNBT data owned by maps or custom systems.", "由地图或自定义系统使用的 SNBT 数据。", List.of("{}"), "{<key>:<value>}", "minecraft:custom_data={example:1}"));
        add(docs, item("max_stack_size", "Maximum stack size from 1 to 99.", "最大堆叠数量，范围 1 到 99。", List.of("1", "16", "64", "99"), "64", "minecraft:max_stack_size=64"));
        add(docs, item("max_damage", "Positive maximum durability. Usually paired with damage.", "正数最大耐久，通常与 damage 一起使用。", List.of("1", "100", "1000"), "100", "minecraft:max_damage=100"));
        add(docs, item("damage", "Current non-negative durability damage.", "当前非负耐久损耗值。", List.of("0", "1", "50"), "0", "minecraft:damage=0"));
        add(docs, item("unbreakable", "Prevents durability loss; show_in_tooltip controls its tooltip line.", "防止耐久损耗；show_in_tooltip 控制提示行。", List.of("{}", "{show_in_tooltip:false}"), "{show_in_tooltip:true}", "minecraft:unbreakable={show_in_tooltip:false}"));
        add(docs, item("custom_name", "Custom display name encoded as a JSON text Component string.", "以 JSON 文本 Component 字符串编码的自定义显示名称。", List.of("'{\"text\":\"\"}'"), "'{\"text\":\"<name>\"}'", "minecraft:custom_name='{\"text\":\"Blade\",\"color\":\"aqua\"}'"));
        add(docs, item("item_name", "Base item name Component, distinct from an anvil custom name.", "物品基础名称组件，与铁砧自定义名不同。", List.of("'{\"text\":\"\"}'"), "'{\"text\":\"<name>\"}'", "minecraft:item_name='{\"text\":\"Quest Item\"}'"));
        add(docs, item("lore", "Up to 256 JSON text Component strings shown as lore lines.", "最多 256 行、以 JSON 文本 Component 字符串表示的描述文本。", List.of("[]", "['{\"text\":\"\"}']"), "['{\"text\":\"<line>\"}']", "minecraft:lore=['{\"text\":\"Line 1\"}']"));
        add(docs, item("rarity", "Item rarity controlling the default name color.", "控制默认名称颜色的物品稀有度。", List.of("common", "uncommon", "rare", "epic"), "common", "minecraft:rarity=rare"));
        add(docs, item("enchantments", "Enchantment-to-level map with an optional tooltip flag.", "附魔到等级的映射，可设置是否显示提示。", List.of("{}", "{levels:{\"minecraft:sharpness\":1}}"), "{levels:{\"minecraft:<enchantment>\":<level>},show_in_tooltip:true}", "minecraft:enchantments={levels:{\"minecraft:sharpness\":5}}"));
        add(docs, item("stored_enchantments", "Stored enchantments used by enchanted books.", "附魔书等物品保存的附魔。", List.of("{}", "{levels:{\"minecraft:sharpness\":1}}"), "{levels:{\"minecraft:<enchantment>\":<level>}}", "minecraft:stored_enchantments={levels:{\"minecraft:mending\":1}}"));
        add(docs, item("can_place_on", "Adventure-mode block predicate controlling placement.", "控制冒险模式可放置方块的方块谓词。", List.of("{predicates:[]}"), "{predicates:[{blocks:\"minecraft:<block>\"}],show_in_tooltip:true}", "minecraft:can_place_on={predicates:[{blocks:\"minecraft:stone\"}]}"));
        add(docs, item("can_break", "Adventure-mode block predicate controlling breaking.", "控制冒险模式可破坏方块的方块谓词。", List.of("{predicates:[]}"), "{predicates:[{blocks:\"minecraft:<block>\"}],show_in_tooltip:true}", "minecraft:can_break={predicates:[{blocks:\"minecraft:stone\"}]}"));
        add(docs, item("attribute_modifiers", "Attribute modifier entries and tooltip visibility.", "属性修饰符条目及提示可见性。", List.of("[]", "{modifiers:[]}"), "{modifiers:[<modifier>],show_in_tooltip:true}", "minecraft:attribute_modifiers={modifiers:[]}"));
        add(docs, item("custom_model_data", "Integer selected by custom model predicates.", "供自定义模型谓词选择的整数。", List.of("0", "1", "100"), "0", "minecraft:custom_model_data=1"));
        for (String unit : List.of("hide_additional_tooltip", "hide_tooltip", "creative_slot_lock", "intangible_projectile", "fire_resistant")) {
            add(docs, item(unit, "Presence-only marker component.", "仅通过是否存在来生效的标记组件。", List.of("{}"), "{}", "minecraft:" + unit + "={}"));
        }
        add(docs, item("repair_cost", "Prior-work penalty used by anvils.", "铁砧使用的先前工作惩罚值。", List.of("0", "1", "10"), "0", "minecraft:repair_cost=0"));
        add(docs, item("enchantment_glint_override", "Forces the enchantment glint on or off.", "强制开启或关闭附魔光效。", List.of("true", "false"), "true", "minecraft:enchantment_glint_override=true"));
        add(docs, item("food", "Defines nutrition, saturation, eating time, conversion item, and effects.", "定义营养、饱和度、食用时间、转换物品和效果。", List.of("{nutrition:1,saturation:0.1f}"), "{nutrition:<points>,saturation:<value>,can_always_eat:false,eat_seconds:1.6f}", "minecraft:food={nutrition:4,saturation:0.6f}"));
        add(docs, item("tool", "Mining rules, default speed, and durability cost per block.", "挖掘规则、默认速度及每个方块的耐久消耗。", List.of("{rules:[]}"), "{rules:[],default_mining_speed:1.0f,damage_per_block:1}", "minecraft:tool={rules:[],default_mining_speed:1.0f}"));
        add(docs, item("dyed_color", "RGB item color and tooltip visibility.", "物品 RGB 颜色及提示可见性。", List.of("16711680", "{rgb:16711680,show_in_tooltip:true}"), "{rgb:<decimal_rgb>,show_in_tooltip:true}", "minecraft:dyed_color={rgb:16711680}"));
        add(docs, item("map_color", "ARGB/RGB color used by filled-map item rendering.", "已填充地图物品渲染使用的 ARGB/RGB 颜色。", List.of("16777215"), "<decimal_rgb>", "minecraft:map_color=16711680"));
        add(docs, item("potion_contents", "Potion id plus optional custom color and effects.", "药水 id，以及可选的自定义颜色和效果。", List.of("\"minecraft:water\"", "{potion:\"minecraft:healing\"}"), "{potion:\"minecraft:<potion>\"}", "minecraft:potion_contents={potion:\"minecraft:healing\"}"));
        add(docs, item("trim", "Armor trim material and pattern.", "盔甲纹饰的材料和图案。", List.of("{material:\"minecraft:iron\",pattern:\"minecraft:sentry\"}"), "{material:\"minecraft:<material>\",pattern:\"minecraft:<pattern>\"}", "minecraft:trim={material:\"minecraft:gold\",pattern:\"minecraft:spire\"}"));
        add(docs, item("entity_data", "Entity SNBT applied when an item creates an entity.", "物品生成实体时应用的实体 SNBT。", List.of("{}"), "{<entity_nbt>}", "minecraft:entity_data={id:\"minecraft:pig\"}"));
        add(docs, item("bucket_entity_data", "Entity SNBT preserved by entity buckets.", "实体桶保存的实体 SNBT。", List.of("{}"), "{<entity_nbt>}", "minecraft:bucket_entity_data={NoAI:1b}"));
        add(docs, item("block_entity_data", "Block-entity SNBT applied when the item is placed.", "放置物品时应用的方块实体 SNBT。", List.of("{}"), "{<block_entity_nbt>}", "minecraft:block_entity_data={CustomName:'{\"text\":\"Box\"}'}"));
        add(docs, item("instrument", "Instrument registry id used by goat horns.", "山羊角使用的乐器注册表 id。", List.of("\"minecraft:ponder_goat_horn\""), "\"minecraft:<instrument>\"", "minecraft:instrument=\"minecraft:ponder_goat_horn\""));
        add(docs, item("ominous_bottle_amplifier", "Ominous bottle amplifier from 0 to 4.", "不祥之瓶的等级增幅，范围 0 到 4。", List.of("0", "1", "2", "3", "4"), "0", "minecraft:ominous_bottle_amplifier=4"));
        add(docs, item("profile", "Player profile name, UUID, and optional signed properties.", "玩家档案名称、UUID 及可选签名属性。", List.of("{name:\"Player\"}"), "{name:\"<player>\"}", "minecraft:profile={name:\"Player\"}"));
        add(docs, item("note_block_sound", "Sound id used by player heads on note blocks.", "玩家头颅放在音符盒上时使用的声音 id。", List.of("\"minecraft:block.note_block.harp\""), "\"minecraft:<sound>\"", "minecraft:note_block_sound=\"minecraft:block.note_block.harp\""));
        add(docs, item("base_color", "Base dye color, primarily used by shields.", "基础染料颜色，主要供盾牌使用。", List.of("white", "red", "blue", "black"), "<dye_color>", "minecraft:base_color=red"));
        add(docs, item("container", "Item stacks stored inside a container item.", "容器物品内部保存的物品堆。", List.of("[]"), "[<item_stack>]", "minecraft:container=[]"));
        add(docs, item("block_state", "Block-state properties applied when placing a block item.", "放置方块物品时应用的方块状态属性。", List.of("{}"), "{<property>:<value>}", "minecraft:block_state={facing:\"north\"}"));
        add(docs, item("lock", "Item predicate required to open a lockable container.", "打开可锁定容器所需满足的物品谓词。", List.of("{}"), "{<item_predicate>}", "minecraft:lock={items:\"minecraft:tripwire_hook\"}"));
        add(docs, item("container_loot", "Loot table and seed used to populate a container.", "用于填充容器的战利品表和种子。", List.of("{loot_table:\"minecraft:chests/simple_dungeon\"}"), "{loot_table:\"<namespace:path>\",seed:0L}", "minecraft:container_loot={loot_table:\"minecraft:chests/simple_dungeon\"}"));
        return immutableOrderedMap(docs);
    }

    private static <K, V> Map<K, V> immutableOrderedMap(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static List<String> jsonStrings(String... values) {
        List<String> result = new ArrayList<>(values.length);
        for (String value : values) result.add("\"" + value + "\"");
        return List.copyOf(result);
    }

    private static FieldDoc field(
            String key,
            String english,
            String chinese,
            List<String> values,
            String template,
            String... examples
    ) {
        return new FieldDoc(key, english, chinese, List.copyOf(values), template, List.of(examples));
    }

    private static ItemComponentDoc item(
            String key,
            String english,
            String chinese,
            List<String> values,
            String template,
            String... examples
    ) {
        return new ItemComponentDoc(key, english, chinese, List.copyOf(values), template, List.of(examples));
    }

    private static void add(Map<String, FieldDoc> fields, FieldDoc field) {
        fields.put(field.key(), field);
    }

    private static void add(Map<String, ItemComponentDoc> fields, ItemComponentDoc field) {
        fields.put(field.key(), field);
    }

    private enum ArgumentKind {
        TEXT_COMPONENT,
        ITEM
    }

    private enum JsonKind {
        COMPONENT,
        COMPONENT_ARRAY,
        CLICK_EVENT,
        HOVER_EVENT,
        HOVER_CONTENT,
        SCORE,
        GENERIC
    }

    private enum JsonContainer {
        OBJECT,
        ARRAY
    }

    private enum JsonExpect {
        KEY,
        COLON,
        VALUE,
        SEPARATOR
    }

    private enum JsonPositionType {
        ROOT,
        KEY,
        COLON,
        VALUE,
        SEPARATOR,
        COMPLETE
    }

    private enum JsonTokenType {
        LBRACE,
        RBRACE,
        LBRACKET,
        RBRACKET,
        COLON,
        COMMA,
        STRING,
        ATOM
    }

    private enum ItemPhase {
        KEY,
        VALUE
    }

    private record ArgumentContext(ArgumentKind kind, int start) {
    }

    private record JsonToken(JsonTokenType type, String text, int start, int end, boolean closed) {
    }

    private record PartialToken(String text, int start, boolean quoted) {
        static PartialToken quoted(JsonToken token) {
            return new PartialToken(token.text(), token.start(), true);
        }

        static PartialToken atom(JsonToken token) {
            return new PartialToken(token.text(), token.start(), false);
        }
    }

    private static final class JsonFrame {
        private final JsonContainer container;
        private final JsonKind kind;
        private JsonExpect expect;
        private String key;
        private final Set<String> usedKeys = new HashSet<>();

        private JsonFrame(JsonContainer container, JsonKind kind, JsonExpect expect) {
            this.container = container;
            this.kind = kind;
            this.expect = expect;
        }

        static JsonFrame object(JsonKind kind) {
            return new JsonFrame(JsonContainer.OBJECT, kind, JsonExpect.KEY);
        }

        static JsonFrame array(JsonKind kind) {
            return new JsonFrame(JsonContainer.ARRAY, kind, JsonExpect.VALUE);
        }
    }

    private record JsonPosition(
            JsonPositionType type,
            JsonKind kind,
            JsonContainer container,
            String key,
            Set<String> usedKeys,
            PartialToken partial
    ) {
        static JsonPosition root() {
            return new JsonPosition(JsonPositionType.ROOT, JsonKind.COMPONENT, null, null, Set.of(), null);
        }

        static JsonPosition complete() {
            return new JsonPosition(JsonPositionType.COMPLETE, JsonKind.GENERIC, null, null, Set.of(), null);
        }

        static JsonPosition key(JsonFrame frame, PartialToken partial) {
            return new JsonPosition(JsonPositionType.KEY, frame.kind, frame.container, frame.key, Set.copyOf(frame.usedKeys), partial);
        }

        static JsonPosition colon(JsonFrame frame) {
            return new JsonPosition(JsonPositionType.COLON, frame.kind, frame.container, frame.key, Set.copyOf(frame.usedKeys), null);
        }

        static JsonPosition value(JsonFrame frame, PartialToken partial) {
            return new JsonPosition(JsonPositionType.VALUE, frame.kind, frame.container, frame.key, Set.copyOf(frame.usedKeys), partial);
        }

        static JsonPosition separator(JsonFrame frame) {
            return new JsonPosition(JsonPositionType.SEPARATOR, frame.kind, frame.container, frame.key, Set.copyOf(frame.usedKeys), null);
        }
    }

    private record ItemPosition(
            ItemPhase phase,
            String key,
            String prefix,
            int replacementStart,
            boolean removed,
            Set<String> usedKeys,
            int embeddedComponentStart
    ) {
    }

    private record FieldDoc(
            String key,
            String english,
            String chinese,
            List<String> values,
            String template,
            List<String> examples
    ) {
        String summary() {
            return localized(chinese, english);
        }

        String keySuggestion() {
            return "\"" + key + "\":" + values.getFirst();
        }

        String keyTemplate() {
            return "\"" + key + "\":" + template;
        }

        CommandInsight insight() {
            return new CommandInsight("Component · " + key, summary(), examples, true);
        }
    }

    private record ItemComponentDoc(
            String key,
            String english,
            String chinese,
            List<String> values,
            String template,
            List<String> examples
    ) {
        String summary() {
            return localized(chinese, english);
        }

        CommandInsight insight() {
            return new CommandInsight("Item Component · " + itemComponentId(key), summary(), examples, true);
        }
    }
}
