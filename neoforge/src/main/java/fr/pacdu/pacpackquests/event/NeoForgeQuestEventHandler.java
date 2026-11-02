package fr.pacdu.pacpackquests.event;

import fr.pacdu.pacpackquests.data.QuestEventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class NeoForgeQuestEventHandler {

    private NeoForgeQuestEventHandler() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(NeoForgeQuestEventHandler.class);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && event.getPlayer() instanceof ServerPlayer sp) {
            QuestEventHandler.onBlockBreak(sp, event.getState());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer sp) {
            QuestEventHandler.onEntityKill(sp, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() && event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer sp) {
            QuestEventHandler.onUseBlock(sp, event.getLevel(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide() && event.getHand() == InteractionHand.MAIN_HAND && event.getTarget() != null && event.getEntity() instanceof ServerPlayer sp) {
            QuestEventHandler.onUseEntity(sp, event.getLevel(), event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        QuestEventHandler.onServerTick(event.getServer());
    }
}
