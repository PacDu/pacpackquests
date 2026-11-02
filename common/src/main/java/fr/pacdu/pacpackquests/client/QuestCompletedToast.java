package fr.pacdu.pacpackquests.client;

import fr.pacdu.pacpackquests.QuestDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

public class QuestCompletedToast implements Toast {

    // The vanilla texture used for advancements
    private static final Identifier BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "toast/advancement");

    private final QuestDefinition quest;
    private boolean soundPlayed = false;

    private Visibility visibility = Visibility.SHOW;
    private long startTime = -1;

    public QuestCompletedToast(QuestDefinition quest) {
        this.quest = quest;
    }

    @Override
    public Visibility getWantedVisibility() {
        return this.visibility;
    }

    @Override
    public void update(ToastManager manager, long time) {
        // Initialize start time on the first frame
        if (this.startTime == -1) {
            this.startTime = time;
        }

        // Hide the toast after 5 seconds (5000 ms)
        if (time - this.startTime >= 5000L) {
            this.visibility = Visibility.HIDE;
        }
    }

    @Override
    public void render(GuiGraphics context, Font font, long time) {
        // Play the satisfying achievement sound only once
        if (!this.soundPlayed) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
            }
            this.soundPlayed = true;
        }

        context.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, 0, 0, this.width(), this.height());

        // Draw Title ("Quest Completed!" in yellow)
        Component title = Component.translatable("toast.pacpackquests.completed").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        context.drawString(font, title, 30, 7, 0xFFFFFF00, false);

        // Draw Subtitle (The actual name of the quest in white)
        context.drawString(font, Component.literal(quest.title()), 30, 18, 0xFFFFFFFF, false);

        // Draw the quest icon on the left side
        context.renderFakeItem(quest.icon(), 8, 8);
    }
}
