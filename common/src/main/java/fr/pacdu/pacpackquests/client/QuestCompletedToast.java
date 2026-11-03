package fr.pacdu.pacpackquests.client;

import fr.pacdu.pacpackquests.QuestDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

public class QuestCompletedToast implements Toast {

    // The vanilla texture used for advancements
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "toast/advancement");

    private final QuestDefinition quest;
    private boolean soundPlayed = false;

    public QuestCompletedToast(QuestDefinition quest) {
        this.quest = quest;
    }

    @Override
    public @NotNull Visibility render(@NotNull GuiGraphics context, @NotNull ToastComponent toastComponent, long time) {
        // Play the satisfying achievement sound only once
        if (!this.soundPlayed && time > 0L) {
            toastComponent.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F));
            this.soundPlayed = true;
        }

        context.blitSprite(BACKGROUND_TEXTURE, 0, 0, this.width(), this.height());

        Font font = toastComponent.getMinecraft().font;

        // Draw Title ("Quest Completed!" in yellow)
        Component title = Component.translatable("toast.pacpackquests.completed").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        context.drawString(font, title, 30, 7, 0xFFFFFF00, false);

        // Draw Subtitle (The actual name of the quest in white)
        context.drawString(font, Component.literal(quest.title()), 30, 18, 0xFFFFFFFF, false);

        // Draw the quest icon on the left side
        context.renderFakeItem(quest.icon(), 8, 8);

        return (double) time >= 5000.0D * toastComponent.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
