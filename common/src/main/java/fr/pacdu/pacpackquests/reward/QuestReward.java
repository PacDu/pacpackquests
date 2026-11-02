package fr.pacdu.pacpackquests.reward;

import fr.pacdu.pacpackquests.RewardType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Common sealed interface representing a reward granted to a player upon claiming a completed quest.
 */
public sealed interface QuestReward permits ItemReward, XpReward, LevelReward, CommandReward {

    /**
     * Returns the category type of this reward.
     *
     * @return The {@link RewardType}.
     */
    RewardType type();

    /**
     * Returns the numerical quantity or level of this reward.
     *
     * @return The amount integer.
     */
    int amount();

    /**
     * Grants the reward to the given server player.
     *
     * @param player The {@link ServerPlayer} claiming the quest.
     * @param registryManager The server registry access.
     */
    void grant(ServerPlayer player, RegistryAccess registryManager);

    /**
     * Generates a visual ItemStack representing this reward for display in tooltips and menus.
     *
     * @param registries The registry lookup provider.
     * @return An {@link ItemStack} for GUI rendering.
     */
    ItemStack createIconStack(HolderLookup.Provider registries);

    /**
     * Serializes this reward into a network buffer.
     *
     * @param buf The {@link FriendlyByteBuf} to write into.
     */
    void write(FriendlyByteBuf buf);

    /**
     * Deserializes a polymorphic {@link QuestReward} from a network buffer.
     *
     * @param buf The {@link FriendlyByteBuf} to read from.
     * @return The deserialized {@link QuestReward}.
     */
    static QuestReward read(FriendlyByteBuf buf) {
        RewardType type = buf.readEnum(RewardType.class);
        return switch (type) {
            case ITEM -> ItemReward.read(buf);
            case XP -> XpReward.read(buf);
            case LEVEL -> LevelReward.read(buf);
            case COMMAND -> CommandReward.read(buf);
        };
    }
}
