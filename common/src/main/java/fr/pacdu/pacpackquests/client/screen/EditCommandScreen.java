package fr.pacdu.pacpackquests.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;

public class EditCommandScreen extends Screen {

    private final Screen parent;
    private final String previousCommand;
    private final String previousDescription;
    private final BiConsumer<String, String> onEdited;

    private EditBox commandField, commandDescriptionField;
    private CommandSuggestions commandSuggestor;

    protected EditCommandScreen(Screen parent, String previousCommand, String previousDescription, BiConsumer<String, String> onEdited) {
        super(Component.translatable("gui.pacpackquests.edit_command"));
        this.parent = parent;
        this.previousCommand = previousCommand;
        this.previousDescription = previousDescription;
        this.onEdited = onEdited;
    }

    @Override
    protected void init() {
        super.init();

        int commandFieldWidth = (int) Math.clamp(this.width * 0.8, 200, 2000);

        this.commandField = new EditBox(this.font, (this.width - commandFieldWidth) / 2, 50, commandFieldWidth, 20, Component.empty());
        this.commandField.setMaxLength(16384);
        this.commandField.setValue(this.previousCommand != null ? this.previousCommand : "");
        this.commandField.setHint(Component.translatable("form.pacpackquests.reward_command_placeholder").withStyle(ChatFormatting.DARK_GRAY));
        this.commandField.setResponder(text -> {
            if (this.commandSuggestor != null) {
                this.commandSuggestor.updateCommandInfo();
            }
        });
        this.addRenderableWidget(this.commandField);

        this.commandSuggestor = new CommandSuggestions(this.minecraft, this, this.commandField, this.font, true, true, 0, 7, false, Integer.MIN_VALUE);
        this.commandSuggestor.setAllowSuggestions(true);
        this.commandSuggestor.updateCommandInfo();
        this.setFocused(this.commandField);

        this.commandDescriptionField = new EditBox(this.font, (this.width - commandFieldWidth) / 2, this.commandField.getY() + 120, commandFieldWidth, 20, Component.empty());
        this.commandDescriptionField.setMaxLength(256);
        this.commandDescriptionField.setValue(this.previousDescription != null ? this.previousDescription : "");
        this.addRenderableWidget(this.commandDescriptionField);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.pacpackquests.cancel"), button -> this.minecraft.setScreen(this.parent))
                .bounds((this.width - 100) / 2 - 60, this.height - 30, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.pacpackquests.validate"), button -> {
            this.onEdited.accept(this.commandField.getValue(), this.commandDescriptionField.getValue());
            this.minecraft.setScreen(this.parent);
        }).bounds((this.width - 100) / 2 + 60, this.height - 30, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);

        context.drawString(this.font, Component.translatable("form.pacpackquests.command").append(Component.literal(" *").withStyle(ChatFormatting.RED)), this.commandField.getX(), this.commandField.getY() - 10, 0xFFFFFFFF, true);
        context.drawString(this.font, Component.translatable("form.pacpackquests.command_description"), this.commandDescriptionField.getX(), this.commandDescriptionField.getY() - 10, 0xFFFFFFFF, true);

        if (this.commandField.isFocused()) {
            this.commandSuggestor.render(context, mouseX, mouseY);
        } else {
            this.commandField.setSuggestion(null);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (this.commandField.isFocused() && this.commandSuggestor.keyPressed(input)) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (this.commandField.isFocused() && this.commandSuggestor.mouseClicked(click)) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.commandField.isFocused() && this.commandSuggestor.mouseScrolled(verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void setFocused(GuiEventListener focused) {
        super.setFocused(focused);
        if (this.commandSuggestor != null) {
            boolean isCommandFocused = (focused == this.commandField);
            this.commandSuggestor.setAllowSuggestions(isCommandFocused);
            if (isCommandFocused) {
                this.commandSuggestor.updateCommandInfo();
            } else {
                this.commandField.setSuggestion(null);
            }
        }
    }
}
