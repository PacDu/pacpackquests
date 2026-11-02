package fr.pacdu.pacpackquests.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.pacdu.pacpackquests.QuestDefinition;
import fr.pacdu.pacpackquests.network.QuestCompletedPayload;
import fr.pacdu.pacpackquests.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/**
 * Represents the persistent save data for all players' quest progress, claims, and structure discoveries.
 * This state is saved to the world's data storage (data/pacpackquests_data.dat).
 */
public class QuestState extends SavedData {

    /** Player UUID -> (Quest ID -> Progress count) */
    public final Map<UUID, Map<String, Integer>> progress = new HashMap<>();

    /** Player UUID -> (Quest ID -> Claimed status) */
    public final Map<UUID, Map<String, Boolean>> claimed = new HashMap<>();

    /** Player UUID -> (Quest ID -> Set of discovered structure chunk coordinate longs) */
    public final Map<UUID, Map<String, Set<Long>>> discoveredStructures = new HashMap<>();

    /**
     * Gets the current progress value for a quest by player UUID.
     *
     * @param player The player UUID.
     * @param questId The quest identifier.
     * @return The current progress count.
     */
    public int getProgress(UUID player, String questId) {
        return progress.getOrDefault(player, new HashMap<>()).getOrDefault(questId, 0);
    }

    /**
     * Updates the progress of a quest for a player and triggers completion toast if newly completed.
     *
     * @param player The {@link ServerPlayer}.
     * @param questId The quest ID.
     * @param amount The new progress value.
     */
    public void setProgress(ServerPlayer player, String questId, int amount) {
        QuestDefinition quest = QuestManager.LOADED_QUESTS.get(questId);
        if (quest == null) return;

        int oldProgress = this.getProgress(player.getUUID(), questId);

        // Save the new progress
        progress.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(questId, amount);
        this.setDirty();

        // Check if the quest just crossed the completion threshold
        if (oldProgress < quest.requiredAmount() && amount >= quest.requiredAmount()) {
            // Trigger the achievement toast on the client!
            Services.NETWORK.sendToPlayer(player, new QuestCompletedPayload(questId));
        }
    }

    /**
     * Checks if a player has reached the required amount to finish a quest.
     *
     * @param player The player UUID.
     * @param questId The quest ID.
     * @return {@code true} if completed; {@code false} otherwise.
     */
    public boolean isFinished(UUID player, String questId) {
        QuestDefinition quest = QuestManager.LOADED_QUESTS.get(questId);
        if (quest == null) return false;
        return getProgress(player, questId) >= quest.requiredAmount();
    }

    /**
     * Checks if a player has already claimed the rewards for a quest.
     *
     * @param player The player UUID.
     * @param questId The quest ID.
     * @return {@code true} if claimed; {@code false} otherwise.
     */
    public boolean isClaimed(UUID player, String questId) {
        return claimed.getOrDefault(player, new HashMap<>()).getOrDefault(questId, false);
    }

    /**
     * Updates the claimed status of a quest for a player.
     *
     * @param player The player UUID.
     * @param questId The quest ID.
     * @param isClaimed {@code true} if claimed; {@code false} otherwise.
     */
    public void setClaimed(UUID player, String questId, boolean isClaimed) {
        claimed.computeIfAbsent(player, k -> new HashMap<>()).put(questId, isClaimed);
        this.setDirty();
    }

    /**
     * Marks a quest as claimed for a player.
     *
     * @param player The player UUID.
     * @param questId The quest ID.
     */
    public void setClaimed(UUID player, String questId) {
        setClaimed(player, questId, true);
    }

    /**
     * Checks if a player has already discovered a structure instance by chunk coordinate.
     *
     * @param player The player UUID.
     * @param questId The quest ID.
     * @param structureChunkLong The chunk coordinates as a packed long.
     * @return {@code true} if discovered; {@code false} otherwise.
     */
    public boolean hasDiscoveredStructure(UUID player, String questId, long structureChunkLong) {
        return discoveredStructures.getOrDefault(player, new HashMap<>())
                .getOrDefault(questId, new HashSet<>())
                .contains(structureChunkLong);
    }

    /**
     * Adds a structure instance chunk coordinate to the player's discovered set.
     *
     * @param player The player UUID.
     * @param questId The quest ID.
     * @param structureChunkLong The chunk coordinates as a packed long.
     */
    public void addDiscoveredStructure(UUID player, String questId, long structureChunkLong) {
        discoveredStructures.computeIfAbsent(player, k -> new HashMap<>())
                .computeIfAbsent(questId, k -> new HashSet<>())
                .add(structureChunkLong);
        this.setDirty();
    }

    /**
     * Deletes all progress, claims, and discovery tracking for a deleted quest across all players.
     *
     * @param questId The quest ID to purge.
     */
    public void deleteQuest(String questId) {
        progress.values().forEach(map -> map.remove(questId));
        claimed.values().forEach(map -> map.remove(questId));
        discoveredStructures.values().forEach(map -> map.remove(questId));
        this.setDirty();
    }

    /**
     * Mojang Codec definition for serializing and deserializing QuestState to/from NBT/Dat.
     */
    public static final Codec<QuestState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.INT))
                    .xmap(
                            map -> {
                                Map<UUID, Map<String, Integer>> result = new HashMap<>();
                                map.forEach((k, v) -> result.put(UUID.fromString(k), new HashMap<>(v)));
                                return result;
                            },
                            map -> {
                                Map<String, Map<String, Integer>> result = new HashMap<>();
                                map.forEach((k, v) -> result.put(k.toString(), v));
                                return result;
                            }
                    ).fieldOf("progress").forGetter(state -> state.progress),
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.BOOL))
                    .xmap(
                            map -> {
                                Map<UUID, Map<String, Boolean>> result = new HashMap<>();
                                map.forEach((k, v) -> result.put(UUID.fromString(k), new HashMap<>(v)));
                                return result;
                            },
                            map -> {
                                Map<String, Map<String, Boolean>> result = new HashMap<>();
                                map.forEach((k, v) -> result.put(k.toString(), v));
                                return result;
                            }
                    ).fieldOf("claimed").forGetter(state -> state.claimed),
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.list(Codec.LONG)))
                    .xmap(
                            map -> {
                                Map<UUID, Map<String, Set<Long>>> result = new HashMap<>();
                                map.forEach((k, v) -> {
                                    Map<String, Set<Long>> questMap = new HashMap<>();
                                    v.forEach((qId, list) -> questMap.put(qId, new HashSet<>(list)));
                                    result.put(UUID.fromString(k), questMap);
                                });
                                return result;
                            },
                            map -> {
                                Map<String, Map<String, List<Long>>> result = new HashMap<>();
                                map.forEach((k, v) -> {
                                    Map<String, List<Long>> questMap = new HashMap<>();
                                    v.forEach((qId, set) -> questMap.put(qId, new ArrayList<>(set)));
                                    result.put(k.toString(), questMap);
                                });
                                return result;
                            }
                    ).optionalFieldOf("discoveredStructures", new HashMap<>()).forGetter(state -> state.discoveredStructures)
    ).apply(instance, (prog, claim, disc) -> {
        QuestState state = new QuestState();
        state.progress.putAll(prog);
        state.claimed.putAll(claim);
        state.discoveredStructures.putAll(disc);
        return state;
    }));

    /**
     * SavedDataType registration for the Minecraft Server data storage.
     */
    public static final SavedDataType<QuestState> TYPE = new SavedDataType<>(
            "pacpackquests_data",
            QuestState::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_DATA
    );

    /**
     * Retrieves the persistent {@link QuestState} instance from the server's overworld data storage.
     *
     * @param server The {@link MinecraftServer}.
     * @return The loaded or newly created {@link QuestState}.
     */
    public static QuestState getServerState(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
