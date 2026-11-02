package fr.pacdu.pacpackquests.data;

import fr.pacdu.pacpackquests.QuestDefinition;
import fr.pacdu.pacpackquests.network.QuestProgressPayload;
import fr.pacdu.pacpackquests.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

public class QuestProgressHandler {

    public static void incrementProgress(ServerPlayer player, QuestDefinition quest, int amountToAdd) {
        setProgress(player, quest, amountToAdd, true);
    }

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
