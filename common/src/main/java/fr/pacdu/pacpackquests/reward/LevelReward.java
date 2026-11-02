package fr.pacdu.pacpackquests.reward;

import fr.pacdu.pacpackquests.RewardType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A quest reward that grants experience levels directly to the player.
 *
 * @param amount The number of experience levels to add.
 */
public record LevelReward(int amount) implements QuestReward {

    @Override
    public RewardType type() {
        return RewardType.LEVEL;
    }

    @Override
    public void grant(ServerPlayer player, RegistryAccess registryManager) {
        if (player != null && amount > 0) {
            player.giveExperienceLevels(amount);
        }
    }

    @Override
    public ItemStack createIconStack(HolderLookup.Provider registries) {
        return new ItemStack(Items.EXPERIENCE_BOTTLE);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(RewardType.LEVEL);
        buf.writeInt(this.amount);
    }

    /**
     * Reads a {@link LevelReward} from the network buffer.
     *
     * @param buf The buffer to read from.
     * @return The read {@link LevelReward}.
     */
    public static LevelReward read(FriendlyByteBuf buf) {
        return new LevelReward(buf.readInt());
    }
}
