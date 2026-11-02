package fr.pacdu.pacpackquests.reward;

import fr.pacdu.pacpackquests.RewardType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A quest reward that grants flat experience points directly to the player.
 *
 * @param amount The number of experience points to grant.
 */
public record XpReward(int amount) implements QuestReward {

    @Override
    public RewardType type() {
        return RewardType.XP;
    }

    @Override
    public void grant(ServerPlayer player, RegistryAccess registryManager) {
        if (player != null && amount > 0) {
            player.giveExperiencePoints(amount);
        }
    }

    @Override
    public ItemStack createIconStack(HolderLookup.Provider registries) {
        return new ItemStack(Items.EXPERIENCE_BOTTLE);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(RewardType.XP);
        buf.writeInt(this.amount);
    }

    /**
     * Reads an {@link XpReward} from the network buffer.
     *
     * @param buf The buffer to read from.
     * @return The read {@link XpReward}.
     */
    public static XpReward read(FriendlyByteBuf buf) {
        return new XpReward(buf.readInt());
    }
}
