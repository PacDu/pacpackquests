package fr.pacdu.pacpackquests.mixin;

import fr.pacdu.pacpackquests.QuestDefinition;
import fr.pacdu.pacpackquests.TaskType;
import fr.pacdu.pacpackquests.data.QuestManager;
import fr.pacdu.pacpackquests.data.QuestProgressHandler;
import fr.pacdu.pacpackquests.util.ItemUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public class CraftingResultSlotMixin {

    @Shadow @Final private Player player;
    @Shadow private int removeCount;

    @Inject(method = "checkTakeAchievements(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void onCraftedItem(ItemStack stack, CallbackInfo ci) {
        if (!this.player.level().isClientSide() && this.player instanceof ServerPlayer serverPlayer) {
            if (stack.isEmpty()) return;

            for (QuestDefinition quest : QuestManager.LOADED_QUESTS.values()) {
                if (quest.taskType() == TaskType.CRAFT_ITEM) {
                    String target = quest.target().trim();
                    boolean isTarget = ItemUtils.matches(stack, target, serverPlayer.level().registryAccess());
                    if (isTarget) {
                        int count = this.removeCount > 0 ? this.removeCount : stack.getCount();
                        QuestProgressHandler.incrementProgress(serverPlayer, quest, count);
                    }
                }
            }
        }
    }
}
