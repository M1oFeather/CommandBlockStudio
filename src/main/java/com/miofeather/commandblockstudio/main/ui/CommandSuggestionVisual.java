package com.miofeather.commandblockstudio.main.ui;

import com.miofeather.commandblockstudio.main.insight.CommandInsightService;
import com.miofeather.commandblockstudio.mixin.ParticleEngineAccessor;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

final class CommandSuggestionVisual {
    private final String suggestion;
    private final ItemStack item;
    private final PlayerSkin skin;
    private final PlayerInfo player;
    private final ResourceLocation particleId;
    private final SpriteSet particleSprites;

    private CommandSuggestionVisual(
            String suggestion,
            ItemStack item,
            PlayerSkin skin,
            PlayerInfo player,
            ResourceLocation particleId,
            SpriteSet particleSprites
    ) {
        this.suggestion = suggestion;
        this.item = item;
        this.skin = skin;
        this.player = player;
        this.particleId = particleId;
        this.particleSprites = particleSprites;
    }

    static Optional<CommandSuggestionVisual> resolve(
            Minecraft minecraft,
            CommandInsightService insightService,
            String command,
            int cursor,
            String suggestion
    ) {
        ResourceLocation id = ResourceLocation.tryParse(suggestion);
        if (id != null && insightService.isBlockSuggestion(command, cursor, suggestion)) {
            ItemStack stack = new ItemStack(BuiltInRegistries.BLOCK.get(id).asItem());
            if (!stack.isEmpty()) {
                return Optional.of(item(suggestion, stack));
            }
        }
        if (id != null && insightService.isItemSuggestion(command, cursor, suggestion)) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            if (!stack.isEmpty()) {
                return Optional.of(item(suggestion, stack));
            }
        }
        if (id != null && insightService.isEnchantmentSuggestion(command, cursor, suggestion)) {
            return Optional.of(item(suggestion, new ItemStack(Items.ENCHANTED_BOOK)));
        }
        if (id != null && insightService.isParticleSuggestion(command, cursor, suggestion)) {
            SpriteSet sprites = ((ParticleEngineAccessor) minecraft.particleEngine).getSpriteSets().get(id);
            return Optional.of(new CommandSuggestionVisual(
                    suggestion,
                    ItemStack.EMPTY,
                    null,
                    null,
                    id,
                    sprites
            ));
        }
        if (insightService.isPlayerSuggestion(command, cursor, suggestion)) {
            PlayerInfo online = minecraft.getConnection() == null
                    ? null
                    : minecraft.getConnection().getPlayerInfo(suggestion);
            PlayerSkin playerSkin = online != null
                    ? online.getSkin()
                    : DefaultPlayerSkin.get(UUID.nameUUIDFromBytes(
                            ("OfflinePlayer:" + suggestion).getBytes(StandardCharsets.UTF_8)
                    ));
            return Optional.of(new CommandSuggestionVisual(
                    suggestion,
                    ItemStack.EMPTY,
                    playerSkin,
                    online,
                    null,
                    null
            ));
        }
        return Optional.empty();
    }

    private static CommandSuggestionVisual item(String suggestion, ItemStack stack) {
        return new CommandSuggestionVisual(suggestion, stack, null, null, null, null);
    }

    static int playerRank(Minecraft minecraft, String suggestion) {
        if (minecraft.getConnection() == null) {
            return 2;
        }
        if (minecraft.getConnection().getPlayerInfo(suggestion) != null) {
            return 0;
        }
        return suggestion.startsWith("@") ? 1 : 2;
    }

    void renderIcon(GuiGraphics graphics, int x, int y, int size) {
        if (skin != null) {
            PlayerFaceRenderer.draw(graphics, skin, x, y, size);
            return;
        }
        if (particleId != null) {
            TextureAtlasSprite sprite = particleSprite();
            if (sprite != null) {
                graphics.blit(x, y, 0, size, size, sprite);
            } else {
                renderFallbackParticle(graphics, particleId, x, y, size);
            }
            return;
        }
        if (!item.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0.0F);
            float scale = size / 16.0F;
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.renderItem(item, 0, 0);
            graphics.pose().popPose();
        }
    }

    TextureAtlasSprite particleSprite() {
        if (particleSprites == null) {
            return null;
        }
        int frame = (int) ((Util.getMillis() / 100L) % 20L);
        try {
            return particleSprites.get(frame, 20);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static void renderFallbackParticle(GuiGraphics graphics, ResourceLocation id, int x, int y, int size) {
        int hash = id.hashCode();
        int color = 0xFF000000 | (hash & 0x00BFBFBF) | 0x00303030;
        int pulse = Math.max(1, size / 5);
        int center = size / 2;
        graphics.fill(x + center - pulse, y + center - pulse, x + center + pulse + 1, y + center + pulse + 1, color);
        graphics.fill(x + 1, y + center, x + 1 + pulse, y + center + 1, color);
        graphics.fill(x + size - pulse - 1, y + center, x + size - 1, y + center + 1, color);
        graphics.fill(x + center, y + 1, x + center + 1, y + 1 + pulse, color);
    }

    String suggestion() {
        return suggestion;
    }

    ItemStack item() {
        return item;
    }

    PlayerSkin skin() {
        return skin;
    }

    PlayerInfo player() {
        return player;
    }

    ResourceLocation particleId() {
        return particleId;
    }
}
