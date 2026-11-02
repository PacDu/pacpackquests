package fr.pacdu.pacpackquests.data;

import fr.pacdu.pacpackquests.QuestDefinition;
import fr.pacdu.pacpackquests.network.QuestProgressPayload;
import fr.pacdu.pacpackquests.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * Handles server-side progression updates, prerequisite dependency validation, and client packet synchronization.
 */
public class QuestProgressHandler {

    private QuestProgressHandler() {}

    /**
     * Increments the current progress of a quest for a player by a given delta, checking parent dependencies.
     *
     * @param player The {@link ServerPlayer} making progress.
     * @param quest The {@link QuestDefinition} being progressed.
     * @param amountToAdd The quantity to add to current progress.
     */
    public static void incrementProgress(ServerPlayer player, QuestDefinition quest, int amountToAdd) {
        setProgress(player, quest, amountToAdd, true);
    }

    /**
     * Sets the absolute progress value of a quest for a player, clamping to max required amount.
     *
     * @param player The {@link ServerPlayer}.
     * @param quest The {@link QuestDefinition}.
     * @param amountToSet The absolute progress amount to set.
     */
    public static void setProgress(ServerPlayer player, QuestDefinition quest, int amountToSet) {
        setProgress(player, quest, amountToSet, false);
    }

    // Generic method to safely set progress and sync it with the client
    private static void setProgress(ServerPlayer player, QuestDefinition quest, int amount, boolean isIncrementation) {
        MinecraftServer server = Objects.requireNonNull(player.level().getServer());
        QuestState state = QuestState.getServerState(server);
        UUID playerId = player.getUUID();

        // Dependency Check: Block progression if any parent is not finished yet
        for (String parentId : quest.parents()) {
            if (!state.isFinished(playerId, parentId)) {
                return;
            }
        }

        int current = state.getProgress(playerId, quest.id());
        if (isIncrementation) amount = current + amount;
        int newProgress = Math.clamp(amount, current, quest.requiredAmount());

        if (current < quest.requiredAmount() && newProgress != current) {
            boolean isFinished = newProgress >= quest.requiredAmount();
            state.setProgress(player, quest.id(), newProgress);

            Services.NETWORK.sendToPlayer(player, new QuestProgressPayload(quest.id(), newProgress, isFinished, false));
        }
    }
}
