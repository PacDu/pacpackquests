package fr.pacdu.pacpackquests.event;

import fr.pacdu.pacpackquests.data.QuestEventHandler;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

public class FabricQuestEventHandler {

    private FabricQuestEventHandler() {}

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer sp) {
                QuestEventHandler.onBlockBreak(sp, state);
            }
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof ServerPlayer sp) {
                QuestEventHandler.onEntityKill(sp, killedEntity);
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
                QuestEventHandler.onUseBlock(sp, world, hitResult.getBlockPos());
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide() && hand == InteractionHand.MAIN_HAND && hitResult != null && player instanceof ServerPlayer sp) {
                QuestEventHandler.onUseEntity(sp, world, entity);
            }
            return InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(QuestEventHandler::onServerTick);
    }
}
