package com.miofeather.commandblockstudio.mixin;

import com.miofeather.commandblockstudio.main.ui.ChatCommandAssistantPanel;
import com.miofeather.commandblockstudio.main.ui.MultiLineCommandSuggestor;
import com.miofeather.commandblockstudio.main.ui.MultiLineTextFieldWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Shadow
    protected EditBox input;

    @Shadow
    CommandSuggestions commandSuggestions;

    @Unique
    private MultiLineCommandSuggestor commandBlockStudio$chatSuggestor;

    @Unique
    private ChatCommandAssistantPanel commandBlockStudio$assistantPanel;

    @Unique
    private MultiLineTextFieldWidget commandBlockStudio$commandEditor;

    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void commandBlockStudio$installChatAssistant(CallbackInfo callbackInfo) {
        if (minecraft == null) {
            return;
        }
        String initialValue = input.getValue();
        removeWidget(input);

        commandBlockStudio$commandEditor = new MultiLineTextFieldWidget(
                font,
                4,
                height - 12,
                width - 4,
                12,
                Component.translatable("chat.editBox"),
                null
        );
        commandBlockStudio$commandEditor.setMaxLength(256);
        commandBlockStudio$commandEditor.setCanLoseFocus(false);
        commandBlockStudio$commandEditor.setBordered(false);
        commandBlockStudio$commandEditor.setRawText(initialValue);
        commandBlockStudio$commandEditor.setExternalSuggestorRendering(false);
        commandBlockStudio$commandEditor.setExternalDockedInsightPanel(true);
        input = commandBlockStudio$commandEditor;
        addWidget(commandBlockStudio$commandEditor);
        setInitialFocus(commandBlockStudio$commandEditor);
        commandBlockStudio$commandEditor.setFocused(true);

        commandBlockStudio$chatSuggestor = new MultiLineCommandSuggestor(
                minecraft,
                this,
                input,
                font,
                false,
                false,
                1,
                10,
                true,
                -805306368
        );
        commandBlockStudio$commandEditor.setCommandSuggestor(commandBlockStudio$chatSuggestor);
        commandBlockStudio$commandEditor.setResponder(value -> {
            commandBlockStudio$chatSuggestor.setAllowSuggestions(true);
            commandBlockStudio$chatSuggestor.updateCommandInfo();
        });
        commandBlockStudio$commandEditor.refreshFormatting();
        commandBlockStudio$chatSuggestor.setAllowSuggestions(true);
        commandBlockStudio$chatSuggestor.updateCommandInfo();
        commandSuggestions = commandBlockStudio$chatSuggestor;
        commandBlockStudio$assistantPanel = new ChatCommandAssistantPanel(
                font,
                input,
                commandBlockStudio$chatSuggestor
        );
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            )
    )
    private void commandBlockStudio$renderChatAssistant(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        if (commandBlockStudio$assistantPanel == null) {
            return;
        }
        boolean commandMode = input.getValue().startsWith("/");
        commandBlockStudio$commandEditor.setCommandEditorPresentation(commandMode);
        if (!commandMode) {
            commandBlockStudio$commandEditor.setBordered(false);
            commandBlockStudio$commandEditor.setX(4);
            commandBlockStudio$commandEditor.setY(height - 12);
            commandBlockStudio$commandEditor.setWidth(Math.max(1, width - 4));
            commandBlockStudio$commandEditor.setHeight(12);
            return;
        }

        int panelWidth = Math.min(320, Math.max(150, width / 4));
        int panelX = width - panelWidth - 6;
        int panelHeight = Math.max(90, height - 30);
        commandBlockStudio$assistantPanel.setBounds(panelX, 6, panelWidth, panelHeight);

        int editorWidth = Math.max(120, Math.min(width / 2, panelX - 12));
        int editorHeight = Math.max(96, Math.min(210, height / 2 - 8));
        int editorX = 6;
        int editorY = height - editorHeight - 6;
        int headerHeight = 19;
        graphics.fill(editorX, editorY, editorX + editorWidth, editorY + headerHeight, 0xF2181C21);
        graphics.fill(editorX, editorY + headerHeight - 1, editorX + editorWidth, editorY + headerHeight, 0xFF343A42);
        graphics.drawString(
                font,
                Component.translatable("cbs.chatEditor.title"),
                editorX + 7,
                editorY + 6,
                0xFFDDE2E7
        );
        Component length = Component.translatable("cbs.editor.characters", input.getValue().length());
        graphics.drawString(
                font,
                length,
                editorX + editorWidth - font.width(length) - 7,
                editorY + 6,
                0xFF707A85
        );

        commandBlockStudio$commandEditor.setBordered(true);
        commandBlockStudio$commandEditor.setX(editorX);
        commandBlockStudio$commandEditor.setY(editorY + headerHeight);
        commandBlockStudio$commandEditor.setWidth(editorWidth);
        commandBlockStudio$commandEditor.setHeight(editorHeight - headerHeight);
        commandBlockStudio$commandEditor.refreshSuggestorPos();
        commandBlockStudio$assistantPanel.render(graphics, mouseX, mouseY, partialTick);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/CommandSuggestions;render(Lnet/minecraft/client/gui/GuiGraphics;II)V"
            )
    )
    private void commandBlockStudio$avoidVanillaSuggestionLayer(
            CommandSuggestions suggestions,
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        if (commandBlockStudio$commandEditor != null && input.getValue().startsWith("/")) {
            return;
        }
        suggestions.render(graphics, mouseX, mouseY);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void commandBlockStudio$handleEditorKeys(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        boolean commandMode = commandBlockStudio$commandEditor != null
                && input.getValue().startsWith("/");
        if (commandMode && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            if (hasShiftDown()) {
                input.insertText("\n");
            } else {
                ChatScreen screen = (ChatScreen) (Object) this;
                screen.handleChatInput(input.getValue(), true);
                if (minecraft != null && minecraft.screen == screen) {
                    minecraft.setScreen(null);
                }
            }
            callbackInfo.setReturnValue(true);
            return;
        }
        if (commandBlockStudio$chatSuggestor != null
                && commandMode
                && keyCode == GLFW.GLFW_KEY_SPACE
                && hasControlDown()) {
            commandBlockStudio$chatSuggestor.showSuggestionsAtCursor();
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void commandBlockStudio$scrollAssistant(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (commandBlockStudio$assistantPanel != null
                && commandBlockStudio$assistantPanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            callbackInfo.setReturnValue(true);
            return;
        }
        if (commandBlockStudio$commandEditor != null
                && input.getValue().startsWith("/")
                && commandBlockStudio$commandEditor.isMouseOver(mouseX, mouseY)
                && commandBlockStudio$commandEditor.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
