package com.miofeather.commandblockstudio.main.insight;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import com.miofeather.commandblockstudio.mixin.CommandSuggestorAccessor;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public class CommandInsightService {
    private final Minecraft minecraft;
    private final CommandSuggestorAccessor suggestor;
    private ParseResults<SharedSuggestionProvider> cachedSyntaxParse;
    private int cachedSyntaxCursor = -1;
    private boolean cachedSyntaxChinese;
    private Optional<CommandSyntaxHint> cachedSyntaxHint = Optional.empty();

    public CommandInsightService(Minecraft minecraft, CommandSuggestorAccessor suggestor) {
        this.minecraft = minecraft;
        this.suggestor = suggestor;
    }

    public Optional<CommandInsight> describeAt(String command, int cursor) {
        ParseResults<SharedSuggestionProvider> parse = suggestor.getCurrentParse();
        if (parse == null || command.isBlank()) {
            return Optional.empty();
        }

        int index = clamp(cursor, 0, command.length());
        Optional<CommandInsight> structured = StructuredArgumentCompletion.describeCursor(command, index, parse);
        if (structured.isPresent()) {
            return structured;
        }
        String root = findRootCommand(parse).orElseGet(() -> findFirstWord(command).orElse(""));
        Optional<CommandDoc> doc = findAvailableDoc(root, parse);

        if (index == 0 || Character.isWhitespace(command.charAt(index - 1))) {
            Optional<CommandInsight> expected = describeExpected(parse, index, root, doc);
            if (expected.isPresent()) {
                return expected;
            }
        }

        Optional<NamedArgument> argument = findArgumentAt(parse, index);
        if (argument.isPresent()) {
            String name = argument.get().name();
            return Optional.of(describeArgument(root, doc, name, findArgumentNode(parse, name).orElse(null)));
        }

        Optional<ParsedCommandNode<SharedSuggestionProvider>> node = findNodeAt(parse, index);
        if (node.isPresent()) {
            CommandNode<SharedSuggestionProvider> commandNode = node.get().getNode();
            String name = commandNode.getName();
            if (commandNode instanceof LiteralCommandNode) {
                return Optional.of(describeLiteral(root, doc, name));
            }
            if (commandNode instanceof ArgumentCommandNode) {
                return Optional.of(describeArgument(root, doc, name, (ArgumentCommandNode<SharedSuggestionProvider, ?>) commandNode));
            }
        }

        String word = wordAt(command, index);
        if (!word.isBlank()) {
            if (doc.isPresent()) {
                String nodeSummary = doc.get().describeNode(word);
                if (nodeSummary != null) {
                    return Optional.of(new CommandInsight(root + " " + word, nodeSummary, List.of(), true));
                }
                String argumentSummary = doc.get().describeArgument(word);
                if (argumentSummary != null) {
                    return Optional.of(new CommandInsight("<" + word + ">", argumentSummary, List.of(), true));
                }
            }
            return findAvailableDoc(word, parse).map(commandDoc -> new CommandInsight("/" + commandDoc.name(), commandDoc.summary(), commandDoc.examples(), true));
        }

        return doc.map(commandDoc -> new CommandInsight("/" + commandDoc.name(), commandDoc.summary(), commandDoc.examples(), true));
    }

    public Optional<CommandInsight> describeCursor(String command, int cursor) {
        ParseResults<SharedSuggestionProvider> parse = suggestor.getCurrentParse();
        if (parse == null || command.isBlank()) {
            return Optional.empty();
        }

        int index = clamp(cursor, 0, command.length());
        Optional<CommandInsight> structured = StructuredArgumentCompletion.describeCursor(command, index, parse);
        if (structured.isPresent()) {
            return structured;
        }
        String root = findRootCommand(parse).orElseGet(() -> findFirstWord(command).orElse(""));
        Optional<CommandDoc> doc = findAvailableDoc(root, parse);

        if (index == 0 || index == command.length() || Character.isWhitespace(command.charAt(index - 1))) {
            Optional<CommandInsight> expected = describeExpected(parse, index, root, doc);
            if (expected.isPresent()) {
                return expected;
            }
        }

        Optional<NamedArgument> argument = findArgumentAt(parse, Math.max(0, index - 1));
        if (argument.isPresent()) {
            String name = argument.get().name();
            return Optional.of(describeArgument(root, doc, name, findArgumentNode(parse, name).orElse(null)));
        }

        Optional<ParsedCommandNode<SharedSuggestionProvider>> node = findNodeBeforeOrAt(parse, index);
        if (node.isPresent()) {
            String name = node.get().getNode().getName();
            String nodeSummary = doc.map(commandDoc -> commandDoc.describeNode(name)).orElse(null);
            if (nodeSummary != null) {
                return Optional.of(new CommandInsight(root + " " + name, nodeSummary, List.of(), true));
            }
        }

        if (doc.isPresent()) {
            return Optional.of(new CommandInsight("/" + doc.get().name(), doc.get().summary(), doc.get().examples(), true));
        }

        return Optional.empty();
    }

    public Optional<CommandInsight> describeSuggestion(String command, int cursor, String suggestion) {
        ParseResults<SharedSuggestionProvider> parse = suggestor.getCurrentParse();
        if (parse != null) {
            Optional<CommandInsight> structured = StructuredArgumentCompletion.describeSuggestion(
                    command,
                    clamp(cursor, 0, command.length()),
                    suggestion,
                    parse
            );
            if (structured.isPresent()) {
                return structured;
            }
        }

        String normalizedSuggestion = stripNamespace(suggestion);
        Optional<CommandDoc> suggestedCommand = CommandDocLibrary.find(normalizedSuggestion);
        if (suggestedCommand.isPresent()) {
            CommandDoc doc = suggestedCommand.get();
            return Optional.of(new CommandInsight("/" + doc.name(), doc.summary(), doc.examples(), true));
        }

        if (parse == null) {
            return Optional.empty();
        }

        Optional<CommandInsight> richSuggestion = describeRichSuggestion(command, cursor, suggestion);
        if (richSuggestion.isPresent()) {
            return richSuggestion;
        }

        String root = findRootCommand(parse).orElseGet(() -> findFirstWord(command).orElse(""));
        Optional<CommandDoc> doc = findAvailableDoc(root, parse);
        String nodeSummary = doc.map(commandDoc -> commandDoc.describeNode(normalizedSuggestion)).orElse(null);
        if (nodeSummary != null) {
            return Optional.of(new CommandInsight(root + " " + normalizedSuggestion, nodeSummary, List.of(), true));
        }

        SuggestionContext<SharedSuggestionProvider> context = parse.getContext().findSuggestionContext(clamp(cursor, 0, command.length()));
        for (CommandNode<SharedSuggestionProvider> child : context.parent.getChildren()) {
            if (child instanceof LiteralCommandNode && stripNamespace(child.getName()).equals(normalizedSuggestion)) {
                return Optional.of(describeLiteral(root, doc, child.getName()));
            }
        }
        for (CommandNode<SharedSuggestionProvider> child : context.parent.getChildren()) {
            if (child instanceof ArgumentCommandNode<?, ?> argumentNode) {
                @SuppressWarnings("unchecked")
                ArgumentCommandNode<SharedSuggestionProvider, ?> typed =
                        (ArgumentCommandNode<SharedSuggestionProvider, ?>) argumentNode;
                return Optional.of(describeArgument(root, doc, child.getName(), typed));
            }
        }
        return Optional.empty();
    }

    public boolean isBlockSuggestion(String command, int cursor, String suggestion) {
        ResourceLocation id = ResourceLocation.tryParse(suggestion);
        return id != null
                && (expectsArgument(command, cursor, Set.of("BlockStateArgument", "BlockPredicateArgument"), null)
                || expectsArgumentNamed(command, cursor, Set.of("block", "blockstate", "state", "palette")))
                && BuiltInRegistries.BLOCK.containsKey(id);
    }

    public boolean isItemSuggestion(String command, int cursor, String suggestion) {
        ResourceLocation id = ResourceLocation.tryParse(suggestion);
        return id != null
                && (expectsArgument(command, cursor, Set.of("ItemArgument", "ItemPredicateArgument"), null)
                || expectsArgumentNamed(command, cursor, Set.of("item", "itemstack", "stack")))
                && BuiltInRegistries.ITEM.containsKey(id);
    }

    public boolean isEnchantmentSuggestion(String command, int cursor, String suggestion) {
        ResourceLocation id = ResourceLocation.tryParse(suggestion);
        if (id == null || minecraft.level == null
                || !expectsArgument(command, cursor, Set.of("ResourceArgument", "ResourceOrTagArgument"), "enchant")) {
            return false;
        }
        return minecraft.level.registryAccess().registry(Registries.ENCHANTMENT)
                .map(registry -> registry.containsKey(id))
                .orElse(false);
    }

    public boolean isParticleSuggestion(String command, int cursor, String suggestion) {
        ResourceLocation id = ResourceLocation.tryParse(suggestion);
        return id != null
                && (expectsArgument(command, cursor, Set.of("ParticleArgument"), null)
                || expectsArgumentNamed(command, cursor, Set.of("particle", "particletype", "effect")))
                && BuiltInRegistries.PARTICLE_TYPE.containsKey(id);
    }

    public boolean expectsPlayerSuggestions(String command, int cursor) {
        return expectsArgument(
                command,
                cursor,
                Set.of("EntityArgument", "GameProfileArgument", "ScoreHolderArgument"),
                null
        ) || expectsArgumentNamed(command, cursor, Set.of("player", "players", "profile", "target", "targets"));
    }

    public boolean isPlayerSuggestion(String command, int cursor, String suggestion) {
        return expectsPlayerSuggestions(command, cursor)
                && suggestion.matches("[A-Za-z0-9_]{1,16}");
    }

    private Optional<CommandInsight> describeRichSuggestion(String command, int cursor, String suggestion) {
        ResourceLocation id = ResourceLocation.tryParse(suggestion);
        boolean chinese = CommandBlockStudio.useChineseCommandInsight();
        if (id != null && isBlockSuggestion(command, cursor, suggestion)) {
            Block block = BuiltInRegistries.BLOCK.get(id);
            String properties = block.getStateDefinition().getProperties().stream()
                    .map(property -> property.getName())
                    .sorted()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse(chinese ? "无" : "none");
            String summary = chinese
                    ? "方块 " + block.getName().getString() + "；可用方块状态：" + properties
                    : "Block " + block.getName().getString() + "; state properties: " + properties;
            return Optional.of(new CommandInsight(id.toString(), summary, List.of(id.toString()), true));
        }

        if (id != null && isItemSuggestion(command, cursor, suggestion)) {
            String itemName = BuiltInRegistries.ITEM.get(id).getDescription().getString();
            String summary = chinese
                    ? "物品 " + itemName + "；来自当前服务器物品注册表。"
                    : "Item " + itemName + "; from the active server item registry.";
            return Optional.of(new CommandInsight(id.toString(), summary, List.of(id.toString()), true));
        }

        if (id != null && isEnchantmentSuggestion(command, cursor, suggestion) && minecraft.level != null) {
            Optional<Registry<Enchantment>> registry = minecraft.level.registryAccess().registry(Registries.ENCHANTMENT);
            Optional<Enchantment> enchantment = registry.flatMap(value -> value.getOptional(id));
            if (enchantment.isPresent()) {
                Enchantment value = enchantment.get();
                String summary = chinese
                        ? Component.translatable(value.getDescriptionId()).getString() + "；可用等级 1 至 " + value.getMaxLevel()
                        : Component.translatable(value.getDescriptionId()).getString() + "; levels 1 to " + value.getMaxLevel();
                return Optional.of(new CommandInsight(id.toString(), summary, List.of("enchant @s " + id + " 1"), true));
            }
        }

        if (id != null && isParticleSuggestion(command, cursor, suggestion)) {
            String summary = chinese ? "粒子 ID：" + id : "Particle ID: " + id;
            return Optional.of(new CommandInsight(
                    ParticleDisplayNames.get(id),
                    summary,
                    List.of("particle " + id + " ~ ~1 ~"),
                    true
            ));
        }

        if (isPlayerSuggestion(command, cursor, suggestion)) {
            PlayerInfo player = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(suggestion);
            String summary;
            if (player != null) {
                summary = chinese
                        ? "Tab 列表中的在线玩家；延迟 " + player.getLatency() + " ms"
                        : "Online player from the Tab list; latency " + player.getLatency() + " ms";
            } else {
                summary = chinese
                        ? "玩家名称；当前不在 Tab 在线列表中"
                        : "Player name; currently absent from the online Tab list";
            }
            return Optional.of(new CommandInsight(suggestion, summary, List.of(suggestion), true));
        }
        return Optional.empty();
    }

    private boolean expectsArgument(String command, int cursor, Set<String> typeNames, String nameFragment) {
        ParseResults<SharedSuggestionProvider> parse = suggestor.getCurrentParse();
        if (parse == null) {
            return false;
        }
        SuggestionContext<SharedSuggestionProvider> context;
        try {
            context = parse.getContext().findSuggestionContext(clamp(cursor, 0, command.length()));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
        for (CommandNode<SharedSuggestionProvider> child : context.parent.getChildren()) {
            if (!(child instanceof ArgumentCommandNode<?, ?> argumentNode)) {
                continue;
            }
            boolean typeMatches = typeNames.contains(argumentNode.getType().getClass().getSimpleName());
            boolean nameMatches = nameFragment == null
                    || child.getName().toLowerCase().contains(nameFragment.toLowerCase());
            if (typeMatches && nameMatches) {
                return true;
            }
        }
        return false;
    }

    private boolean expectsArgumentNamed(String command, int cursor, Set<String> names) {
        ParseResults<SharedSuggestionProvider> parse = suggestor.getCurrentParse();
        if (parse == null) {
            return false;
        }
        SuggestionContext<SharedSuggestionProvider> context;
        try {
            context = parse.getContext().findSuggestionContext(clamp(cursor, 0, command.length()));
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return false;
        }
        for (CommandNode<SharedSuggestionProvider> child : context.parent.getChildren()) {
            if (!(child instanceof ArgumentCommandNode<?, ?>)) {
                continue;
            }
            String normalized = child.getName().toLowerCase().replace("_", "");
            if (names.stream().anyMatch(name -> normalized.contains(name.toLowerCase().replace("_", "")))) {
                return true;
            }
        }
        return false;
    }

    public Optional<CommandDiagnostic> getDiagnostic(String command) {
        ParseResults<SharedSuggestionProvider> parse = suggestor.getCurrentParse();
        if (parse == null || command.isBlank()) {
            return Optional.empty();
        }

        CommandSyntaxException exception = Commands.getParseException(parse);
        if (exception == null) {
            return Optional.empty();
        }

        if (findSyntaxHint(command, command.length()).isPresent()) {
            return Optional.empty();
        }

        int start = exception.getCursor() >= 0 ? exception.getCursor() : parse.getReader().getCursor();
        start = clamp(start, 0, command.length());
        if (!parse.getReader().canRead() && start >= command.length()) {
            return Optional.empty();
        }
        int end = diagnosticTokenEnd(command, start);
        String message = cleanDiagnosticMessage(ComponentUtils.fromMessage(exception.getRawMessage()).getString());
        return Optional.of(new CommandDiagnostic(start, end, message));
    }

    private static String cleanDiagnosticMessage(String message) {
        return message
                .replace("，错误见下", "")
                .replace("。错误见下", "")
                .replace("错误见下", "")
                .replace(", see below for error", "")
                .replace(". See below for error", "")
                .strip();
    }

    private static int diagnosticTokenEnd(String command, int start) {
        if (start < 0 || start >= command.length()) {
            return Math.max(0, Math.min(start, command.length()));
        }
        char first = command.charAt(start);
        if (Character.isWhitespace(first) || "{}[](),".indexOf(first) >= 0) {
            return start + 1;
        }
        int end = start + 1;
        while (end < command.length()) {
            char current = command.charAt(end);
            if (Character.isWhitespace(current) || "{}[](),".indexOf(current) >= 0) {
                break;
            }
            end++;
        }
        return end;
    }

    public Optional<CommandSyntaxHint> findSyntaxHint(String command, int cursor) {
        ParseResults<SharedSuggestionProvider> parse = suggestor.getCurrentParse();
        if (parse == null || command.isBlank() || cursor < 0 || cursor > command.length()
                || !parse.getReader().getString().equals(command)) {
            return Optional.empty();
        }

        boolean chinese = CommandBlockStudio.useChineseCommandInsight();
        if (parse == cachedSyntaxParse && cursor == cachedSyntaxCursor && chinese == cachedSyntaxChinese) {
            return cachedSyntaxHint;
        }
        Optional<CommandSyntaxHint> hint = StructuredArgumentCompletion.findHint(command, cursor, parse);
        if (hint.isEmpty() && cursor == command.length()) {
            hint = computeSyntaxHint(command, cursor, parse);
        }
        cachedSyntaxParse = parse;
        cachedSyntaxCursor = cursor;
        cachedSyntaxChinese = chinese;
        cachedSyntaxHint = hint;
        return hint;
    }

    public Optional<CommandSyntaxHint> findStructuredSyntaxHint(String command, int cursor) {
        ParseResults<SharedSuggestionProvider> parse = suggestor.getCurrentParse();
        if (parse == null || command.isBlank() || cursor < 0 || cursor > command.length()
                || !parse.getReader().getString().equals(command)) {
            return Optional.empty();
        }
        return StructuredArgumentCompletion.findHint(command, cursor, parse);
    }

    private Optional<CommandSyntaxHint> computeSyntaxHint(
            String command,
            int cursor,
            ParseResults<SharedSuggestionProvider> parse
    ) {
        if (!parse.getReader().canRead() && Commands.getParseException(parse) == null) {
            return Optional.empty();
        }

        int index = clamp(cursor, 0, command.length());
        SuggestionContext<SharedSuggestionProvider> context;
        try {
            context = parse.getContext().findSuggestionContext(index);
        } catch (IllegalStateException ignored) {
            return Optional.empty();
        }

        int argumentStart = clamp(Math.min(context.startPos, index), 0, index);
        String partial = command.substring(argumentStart, index);
        if ((!partial.isEmpty() && Character.isWhitespace(partial.charAt(0)))
                || partial.indexOf('\n') >= 0 || partial.indexOf('\r') >= 0 || partial.indexOf('\t') >= 0) {
            return Optional.empty();
        }

        List<CommandNode<SharedSuggestionProvider>> candidates = new ArrayList<>(context.parent.getChildren());
        if (context.parent.getRedirect() != null) {
            candidates.addAll(context.parent.getRedirect().getChildren());
        }

        for (CommandNode<SharedSuggestionProvider> candidate : candidates) {
            if (!(candidate instanceof ArgumentCommandNode<?, ?> argumentNode)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            ArgumentCommandNode<SharedSuggestionProvider, ?> typed =
                    (ArgumentCommandNode<SharedSuggestionProvider, ?>) argumentNode;
            Optional<CommandSyntaxHint> hint = buildCompositeHint(typed, partial, command, context.parent);
            if (hint.isPresent()) {
                return hint;
            }
        }

        Optional<CommandSyntaxHint> explored = findSyntaxHintByExploring(command);
        if (explored.isPresent()) {
            return explored;
        }

        // Ambiguous commands such as /tp may choose a shorter executable branch as the main parse.
        // Failed sibling branches still identify the intended composite argument and its real start.
        for (Map.Entry<CommandNode<SharedSuggestionProvider>, CommandSyntaxException> failure
                : parse.getExceptions().entrySet()) {
            if (!(failure.getKey() instanceof ArgumentCommandNode<?, ?> argumentNode)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            ArgumentCommandNode<SharedSuggestionProvider, ?> typed =
                    (ArgumentCommandNode<SharedSuggestionProvider, ?>) argumentNode;
            if (compositeSyntax(typed.getType()).isEmpty()) {
                continue;
            }

            int failedArgumentStart = failure.getValue().getCursor();
            if (failedArgumentStart < 0 || failedArgumentStart > index) {
                continue;
            }
            String failedPartial = command.substring(failedArgumentStart, index);
            CommandNode<SharedSuggestionProvider> failedUsageParent = findParentNode(typed).orElse(context.parent);
            Optional<CommandSyntaxHint> hint = buildCompositeHint(
                    typed,
                    failedPartial,
                    command,
                    failedUsageParent
            );
            if (hint.isPresent()) {
                return hint;
            }
        }
        return Optional.empty();
    }

    private Optional<CommandSyntaxHint> findSyntaxHintByExploring(String command) {
        if (minecraft.player == null || minecraft.player.connection == null) {
            return Optional.empty();
        }

        CommandDispatcher<SharedSuggestionProvider> dispatcher = minecraft.player.connection.getCommands();
        SharedSuggestionProvider provider = minecraft.player.connection.getSuggestionsProvider();
        StringReader reader = new StringReader(command);
        if (reader.canRead() && reader.peek() == '/') {
            reader.skip();
        }
        CommandContextBuilder<SharedSuggestionProvider> context = new CommandContextBuilder<>(
                dispatcher,
                provider,
                dispatcher.getRoot(),
                reader.getCursor()
        );
        return exploreSyntaxBranches(dispatcher.getRoot(), reader, context, command, 0, new int[]{0});
    }

    private Optional<CommandSyntaxHint> exploreSyntaxBranches(
            CommandNode<SharedSuggestionProvider> parent,
            StringReader originalReader,
            CommandContextBuilder<SharedSuggestionProvider> contextSoFar,
            String command,
            int depth,
            int[] exploredNodes
    ) {
        if (depth > 24 || exploredNodes[0] >= 128 || !originalReader.canRead()) {
            return Optional.empty();
        }

        SharedSuggestionProvider provider = contextSoFar.getSource();
        for (CommandNode<SharedSuggestionProvider> child : parent.getRelevantNodes(originalReader)) {
            if (!child.canUse(provider) || ++exploredNodes[0] > 128) {
                continue;
            }

            if (child instanceof ArgumentCommandNode<?, ?> argumentNode) {
                @SuppressWarnings("unchecked")
                ArgumentCommandNode<SharedSuggestionProvider, ?> typed =
                        (ArgumentCommandNode<SharedSuggestionProvider, ?>) argumentNode;
                if (compositeSyntax(typed.getType()).isPresent()) {
                    String partial = command.substring(originalReader.getCursor());
                    Optional<CommandSyntaxHint> hint = buildCompositeHint(typed, partial, command, parent);
                    if (hint.isPresent()) {
                        return hint;
                    }
                }
            }

            CommandContextBuilder<SharedSuggestionProvider> context = contextSoFar.copy();
            StringReader reader = new StringReader(originalReader);
            try {
                child.parse(reader, context);
                if (reader.canRead() && reader.peek() != ' ') {
                    continue;
                }
            } catch (CommandSyntaxException | RuntimeException ignored) {
                continue;
            }

            context.withCommand(child.getCommand());
            if (!reader.canRead(child.getRedirect() == null ? 2 : 1)) {
                continue;
            }
            reader.skip();

            if (child.getRedirect() != null) {
                CommandContextBuilder<SharedSuggestionProvider> childContext = new CommandContextBuilder<>(
                        context.getDispatcher(),
                        provider,
                        child.getRedirect(),
                        reader.getCursor()
                );
                Optional<CommandSyntaxHint> hint = exploreSyntaxBranches(
                        child.getRedirect(),
                        reader,
                        childContext,
                        command,
                        depth + 1,
                        exploredNodes
                );
                if (hint.isPresent()) {
                    return hint;
                }
            } else {
                Optional<CommandSyntaxHint> hint = exploreSyntaxBranches(
                        child,
                        reader,
                        context,
                        command,
                        depth + 1,
                        exploredNodes
                );
                if (hint.isPresent()) {
                    return hint;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<CommandSyntaxHint> buildCompositeHint(
            ArgumentCommandNode<SharedSuggestionProvider, ?> node,
            String partial,
            String command,
            CommandNode<SharedSuggestionProvider> usageParent
    ) {
        Optional<CompositeSyntax> syntax = compositeSyntax(node.getType());
        if (syntax.isEmpty()) {
            return Optional.empty();
        }

        String normalized = partial.stripTrailing();
        List<String> entered = normalized.isEmpty() ? List.of() : List.of(normalized.split(" +"));
        CompositeSyntax composite = syntax.get();
        if (entered.size() >= composite.labels().size()) {
            return Optional.empty();
        }
        List<String> suggestions = findCompositeCompletions(
                node.getType(),
                normalized,
                entered.size(),
                composite.labels().size()
        );
        if (suggestions.isEmpty()) {
            return Optional.empty();
        }

        List<String> remaining = new ArrayList<>();
        for (int i = entered.size(); i < composite.labels().size(); i++) {
            remaining.add("<" + composite.labels().get(i) + ">");
        }
        String placeholders = String.join(" ", remaining);
        String separator = command.isEmpty() || Character.isWhitespace(command.charAt(command.length() - 1)) ? "" : " ";
        String insertion = separator + placeholders;

        Map<CommandNode<SharedSuggestionProvider>, String> usages = getSmartUsages(usageParent);
        String available = String.join(" | ", usages.values().stream().limit(3).toList());
        boolean chinese = CommandBlockStudio.useChineseCommandInsight();
        String summary = chinese
                ? "还需 " + placeholders + "；Tab 填入占位，Ctrl+Space 查看可用值；格式 " + composite.chineseForms()
                : "Missing " + placeholders + "; Tab inserts placeholders, Ctrl+Space shows values; forms: " + composite.englishForms();
        if (!available.isBlank()) {
            summary += chinese ? "；可用语法 " + available : "; syntax: " + available;
        }
        int replacementStart = command.length() - partial.length();
        return Optional.of(new CommandSyntaxHint(
                insertion,
                insertion,
                summary,
                replacementStart,
                List.copyOf(suggestions)
        ));
    }

    private List<String> findCompositeCompletions(ArgumentType<?> type, String partial, int enteredCount, int arity) {
        String style = partialStyle(partial);
        List<String> examples = new ArrayList<>(type.getExamples());
        examples.sort(Comparator.comparingInt(example -> exampleStyle(example).equals(style) ? 0 : 1));
        List<String> completions = new ArrayList<>();

        for (String example : examples) {
            String[] exampleParts = example.split(" +");
            if (exampleParts.length < arity) {
                continue;
            }
            StringBuilder candidate = new StringBuilder(partial);
            for (int i = enteredCount; i < arity; i++) {
                if (!candidate.isEmpty()) {
                    candidate.append(' ');
                }
                candidate.append(exampleParts[i]);
            }

            StringReader reader = new StringReader(candidate.toString());
            try {
                type.parse(reader);
                String completion = candidate.toString();
                if (!reader.canRead() && !completions.contains(completion)) {
                    completions.add(completion);
                    if (completions.size() >= 6) {
                        break;
                    }
                }
            } catch (CommandSyntaxException ignored) {
            }
        }
        return completions;
    }

    private Optional<CompositeSyntax> compositeSyntax(ArgumentType<?> type) {
        return switch (type.getClass().getSimpleName()) {
            case "Vec3Argument", "BlockPosArgument" -> Optional.of(new CompositeSyntax(
                    List.of("x", "y", "z"),
                    "x y z / ~ ~ ~ / ^ ^ ^",
                    "x y z / ~ ~ ~ / ^ ^ ^"
            ));
            case "Vec2Argument" -> Optional.of(new CompositeSyntax(
                    List.of("x", "z"),
                    "x z / ~ ~",
                    "x z / ~ ~"
            ));
            case "ColumnPosArgument" -> Optional.of(new CompositeSyntax(
                    List.of("x", "z"),
                    "x z / ~ ~ / ^ ^",
                    "x z / ~ ~ / ^ ^"
            ));
            case "RotationArgument" -> Optional.of(new CompositeSyntax(
                    List.of("yaw", "pitch"),
                    "偏航角 俯仰角 / ~ ~",
                    "yaw pitch / ~ ~"
            ));
            default -> Optional.empty();
        };
    }

    private static String partialStyle(String partial) {
        String stripped = partial.stripLeading();
        return stripped.isEmpty() ? "" : stripped.substring(0, 1);
    }

    private static String exampleStyle(String example) {
        return example.isEmpty() ? "" : example.substring(0, 1);
    }

    public Optional<CommandInsight> describePlaceholder(String selectedText) {
        if (selectedText == null || selectedText.length() < 3
                || selectedText.charAt(0) != '<'
                || selectedText.charAt(selectedText.length() - 1) != '>') {
            return Optional.empty();
        }
        String name = selectedText.substring(1, selectedText.length() - 1);
        if (name.isBlank() || name.indexOf('<') >= 0 || name.indexOf('>') >= 0) {
            return Optional.empty();
        }
        return Optional.of(new CommandInsight(
                "<" + name + ">",
                CommandElementDescriptions.describeArgument(name, null),
                List.of(),
                true
        ));
    }

    public Optional<String> findTemplate(String command) {
        String trimmed = command.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> rootCommand = findFirstWord(trimmed);
        if (rootCommand.isEmpty()) {
            return Optional.empty();
        }

        String root = rootCommand.get();
        String withoutSlash = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        int rootEnd = withoutSlash.indexOf(' ');
        String remainder = rootEnd >= 0 ? withoutSlash.substring(rootEnd).trim() : "";
        if (!remainder.isEmpty()) {
            return Optional.empty();
        }

        ParseResults<SharedSuggestionProvider> parse = suggestor.getCurrentParse();
        Optional<CommandDoc> doc = parse == null ? Optional.empty() : findAvailableDoc(root, parse);
        if (doc.isPresent() && !doc.get().examples().isEmpty()) {
            return Optional.of(doc.get().examples().get(0));
        }
        return buildDynamicTemplate(root);
    }

    private CommandInsight describeLiteral(String root, Optional<CommandDoc> doc, String name) {
        if (doc.isPresent()) {
            CommandDoc commandDoc = doc.get();
            if (commandDoc.name().equals(name)) {
                return new CommandInsight("/" + commandDoc.name(), commandDoc.summary(), commandDoc.examples(), true);
            }
            String summary = commandDoc.describeNode(name);
            if (summary != null) {
                return new CommandInsight(root + " " + name, summary, List.of(), true);
            }
        }

        String genericSummary = CommandElementDescriptions.describeLiteral(name);
        if (genericSummary != null) {
            return new CommandInsight(root + " " + name, genericSummary, List.of(), true);
        }

        if (name.equals(root)) {
            return CommandInsight.undocumented("/" + name, CommandBlockStudio.useChineseCommandInsight() ? "来自当前服务器命令树的命令。" : "Command from the active command tree.");
        }
        return CommandInsight.undocumented(name, CommandBlockStudio.useChineseCommandInsight() ? "命令中的固定字面量分支。" : "Literal command branch.");
    }

    private CommandInsight describeArgument(
            String root,
            Optional<CommandDoc> doc,
            String name,
            ArgumentCommandNode<SharedSuggestionProvider, ?> node
    ) {
        String summary = doc.map(commandDoc -> commandDoc.describeArgument(name)).orElse(null);
        if (summary != null) {
            return new CommandInsight("<" + name + ">", summary, List.of(), true);
        }
        return new CommandInsight(
                "<" + name + ">",
                CommandElementDescriptions.describeArgument(name, node == null ? null : node.getType()),
                List.of(),
                true
        );
    }

    private Optional<CommandInsight> describeExpected(
            ParseResults<SharedSuggestionProvider> parse,
            int cursor,
            String root,
            Optional<CommandDoc> doc
    ) {
        if (minecraft.player == null || minecraft.player.connection == null) {
            return Optional.empty();
        }

        SuggestionContext<SharedSuggestionProvider> context = parse.getContext().findSuggestionContext(cursor);
        CommandNode<SharedSuggestionProvider> usageParent = context.parent;
        String input = parse.getReader().getString();
        if (cursor > 0 && cursor <= input.length() && !Character.isWhitespace(input.charAt(cursor - 1))) {
            usageParent = findNodeEndingAt(parse, cursor)
                    .map(ParsedCommandNode::getNode)
                    .orElse(usageParent);
        }
        Map<CommandNode<SharedSuggestionProvider>, String> usages = getSmartUsages(usageParent);
        if (usages.isEmpty()) {
            return Optional.empty();
        }

        List<String> choices = usages.values().stream().limit(4).toList();
        String summary = (CommandBlockStudio.useChineseCommandInsight() ? "可填写：" : "Expected: ") + String.join("  |  ", choices);
        String title = CommandBlockStudio.useChineseCommandInsight() ? "下一参数" : "Next argument";

        for (CommandNode<SharedSuggestionProvider> node : usages.keySet()) {
            String detail;
            if (node instanceof LiteralCommandNode) {
                detail = doc.map(commandDoc -> commandDoc.describeNode(node.getName())).orElse(null);
                if (detail == null) {
                    detail = CommandElementDescriptions.describeLiteral(node.getName());
                }
            } else {
                detail = doc.map(commandDoc -> commandDoc.describeArgument(node.getName())).orElse(null);
                if (detail == null && node instanceof ArgumentCommandNode<?, ?> argumentNode) {
                    detail = CommandElementDescriptions.describeArgument(node.getName(), argumentNode.getType());
                }
            }
            if (detail != null) {
                summary += "  " + detail;
                break;
            }
        }
        return Optional.of(new CommandInsight(title, summary, List.of(), true));
    }

    private Map<CommandNode<SharedSuggestionProvider>, String> getSmartUsages(
            CommandNode<SharedSuggestionProvider> usageParent
    ) {
        if (minecraft.player == null || minecraft.player.connection == null) {
            return Map.of();
        }
        CommandDispatcher<SharedSuggestionProvider> dispatcher = minecraft.player.connection.getCommands();
        Map<CommandNode<SharedSuggestionProvider>, String> usages = dispatcher.getSmartUsage(
                usageParent,
                minecraft.player.connection.getSuggestionsProvider()
        );
        if (usages.isEmpty() && usageParent.getRedirect() != null) {
            usages = dispatcher.getSmartUsage(
                    usageParent.getRedirect(),
                    minecraft.player.connection.getSuggestionsProvider()
            );
        }
        return usages;
    }

    private Optional<CommandNode<SharedSuggestionProvider>> findParentNode(
            CommandNode<SharedSuggestionProvider> target
    ) {
        if (minecraft.player == null || minecraft.player.connection == null) {
            return Optional.empty();
        }

        Queue<CommandNode<SharedSuggestionProvider>> queue = new ArrayDeque<>();
        Set<CommandNode<SharedSuggestionProvider>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        queue.add(minecraft.player.connection.getCommands().getRoot());
        while (!queue.isEmpty() && visited.size() < 2048) {
            CommandNode<SharedSuggestionProvider> parent = queue.remove();
            if (!visited.add(parent)) {
                continue;
            }
            for (CommandNode<SharedSuggestionProvider> child : parent.getChildren()) {
                if (child == target) {
                    return Optional.of(parent);
                }
                queue.add(child);
            }
            if (parent.getRedirect() != null) {
                queue.add(parent.getRedirect());
            }
        }
        return Optional.empty();
    }

    private Optional<String> buildDynamicTemplate(String root) {
        if (minecraft.player == null || minecraft.player.connection == null) {
            return Optional.empty();
        }

        SharedSuggestionProvider provider = minecraft.player.connection.getSuggestionsProvider();
        CommandNode<SharedSuggestionProvider> rootNode = minecraft.player.connection.getCommands().getRoot().getChild(root);
        if (rootNode == null) {
            return Optional.empty();
        }

        Queue<TemplatePath> queue = new ArrayDeque<>();
        Set<CommandNode<SharedSuggestionProvider>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        queue.add(new TemplatePath(rootNode, List.of(root)));

        while (!queue.isEmpty() && visited.size() < 128) {
            TemplatePath path = queue.remove();
            CommandNode<SharedSuggestionProvider> node = path.node();
            if (!visited.add(node) || path.parts().size() > 12) {
                continue;
            }
            if (node.getCommand() != null) {
                return Optional.of(String.join(" ", path.parts()));
            }

            for (CommandNode<SharedSuggestionProvider> child : node.getChildren()) {
                if (!child.canUse(provider)) {
                    continue;
                }
                List<String> parts = new ArrayList<>(path.parts());
                parts.add(child instanceof LiteralCommandNode ? child.getName() : "<" + child.getName() + ">");
                queue.add(new TemplatePath(child, List.copyOf(parts)));
            }
            if (node.getRedirect() != null) {
                queue.add(new TemplatePath(node.getRedirect(), path.parts()));
            }
        }
        return Optional.empty();
    }

    private Optional<String> findRootCommand(ParseResults<SharedSuggestionProvider> parse) {
        for (ParsedCommandNode<SharedSuggestionProvider> node : flattenNodes(parse.getContext())) {
            if (node.getNode() instanceof LiteralCommandNode && !node.getNode().getName().isBlank()) {
                return Optional.of(node.getNode().getName());
            }
        }
        return Optional.empty();
    }

    private Optional<CommandDoc> findAvailableDoc(String command, ParseResults<SharedSuggestionProvider> parse) {
        Optional<CommandDoc> doc = CommandDocLibrary.find(command);
        if (doc.isEmpty() || !CommandDocLibrary.isModDocumented(command)) {
            return doc;
        }
        String root = command;
        if (root.startsWith("minecraft:")) {
            root = root.substring("minecraft:".length());
        }
        return parse.getContext().getRootNode().getChild(root) == null ? Optional.empty() : doc;
    }

    private Optional<ParsedCommandNode<SharedSuggestionProvider>> findNodeAt(ParseResults<SharedSuggestionProvider> parse, int cursor) {
        for (ParsedCommandNode<SharedSuggestionProvider> node : flattenNodes(parse.getContext())) {
            if (contains(node.getRange(), cursor)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    private Optional<ParsedCommandNode<SharedSuggestionProvider>> findNodeBeforeOrAt(ParseResults<SharedSuggestionProvider> parse, int cursor) {
        ParsedCommandNode<SharedSuggestionProvider> best = null;
        for (ParsedCommandNode<SharedSuggestionProvider> node : flattenNodes(parse.getContext())) {
            if (node.getRange().getStart() <= cursor && (best == null || node.getRange().getStart() >= best.getRange().getStart())) {
                best = node;
            }
        }
        return Optional.ofNullable(best);
    }

    private Optional<ParsedCommandNode<SharedSuggestionProvider>> findNodeEndingAt(
            ParseResults<SharedSuggestionProvider> parse,
            int cursor
    ) {
        ParsedCommandNode<SharedSuggestionProvider> deepest = null;
        for (ParsedCommandNode<SharedSuggestionProvider> node : flattenNodes(parse.getContext())) {
            if (node.getRange().getEnd() == cursor
                    && (deepest == null || node.getRange().getStart() >= deepest.getRange().getStart())) {
                deepest = node;
            }
        }
        return Optional.ofNullable(deepest);
    }

    private Optional<NamedArgument> findArgumentAt(ParseResults<SharedSuggestionProvider> parse, int cursor) {
        CommandContextBuilder<SharedSuggestionProvider> context = parse.getContext();
        while (context != null) {
            for (Map.Entry<String, ParsedArgument<SharedSuggestionProvider, ?>> entry : context.getArguments().entrySet()) {
                if (contains(entry.getValue().getRange(), cursor)) {
                    return Optional.of(new NamedArgument(entry.getKey(), entry.getValue()));
                }
            }
            context = context.getChild();
        }
        return Optional.empty();
    }

    private Optional<ArgumentCommandNode<SharedSuggestionProvider, ?>> findArgumentNode(
            ParseResults<SharedSuggestionProvider> parse,
            String name
    ) {
        for (ParsedCommandNode<SharedSuggestionProvider> parsedNode : flattenNodes(parse.getContext())) {
            if (parsedNode.getNode() instanceof ArgumentCommandNode<?, ?> argumentNode && argumentNode.getName().equals(name)) {
                @SuppressWarnings("unchecked")
                ArgumentCommandNode<SharedSuggestionProvider, ?> typed =
                        (ArgumentCommandNode<SharedSuggestionProvider, ?>) argumentNode;
                return Optional.of(typed);
            }
        }
        return Optional.empty();
    }

    private List<ParsedCommandNode<SharedSuggestionProvider>> flattenNodes(CommandContextBuilder<SharedSuggestionProvider> context) {
        List<ParsedCommandNode<SharedSuggestionProvider>> nodes = new ArrayList<>();
        CommandContextBuilder<SharedSuggestionProvider> current = context;
        while (current != null) {
            nodes.addAll(current.getNodes());
            current = current.getChild();
        }
        return nodes;
    }

    private static boolean contains(StringRange range, int cursor) {
        return cursor >= range.getStart() && cursor <= range.getEnd();
    }

    private static Optional<String> findFirstWord(String command) {
        String trimmed = command.startsWith("/") ? command.substring(1) : command;
        int end = trimmed.indexOf(' ');
        if (end >= 0) {
            trimmed = trimmed.substring(0, end);
        }
        return trimmed.isBlank() ? Optional.empty() : Optional.of(trimmed);
    }

    private static String stripNamespace(String value) {
        return value.startsWith("minecraft:") ? value.substring("minecraft:".length()) : value;
    }

    private static String wordAt(String command, int cursor) {
        if (command.isBlank()) {
            return "";
        }
        int index = clamp(cursor, 0, command.length() - 1);
        if (Character.isWhitespace(command.charAt(index)) && index > 0) {
            index--;
        }

        int start = index;
        while (start > 0 && isWordPart(command.charAt(start - 1))) {
            start--;
        }

        int end = index;
        while (end < command.length() && isWordPart(command.charAt(end))) {
            end++;
        }

        if (start >= end) {
            return "";
        }
        String word = command.substring(start, end);
        return word.startsWith("/") ? word.substring(1) : word;
    }

    private static boolean isWordPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == ':' || c == '/' || c == '-' || c == '.';
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record NamedArgument(String name, ParsedArgument<SharedSuggestionProvider, ?> argument) {
    }

    private record TemplatePath(CommandNode<SharedSuggestionProvider> node, List<String> parts) {
    }

    private record CompositeSyntax(List<String> labels, String chineseForms, String englishForms) {
    }
}
