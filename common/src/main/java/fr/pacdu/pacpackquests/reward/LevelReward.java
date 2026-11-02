package fr.pacdu.pacpackquests.reward;

import fr.pacdu.pacpackquests.RewardType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        return new ItemStack(Items.ENCHANTED_BOOK);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(RewardType.LEVEL);
        buf.writeInt(amount);
    }

    public static LevelReward read(FriendlyByteBuf buf) {
        return new LevelReward(buf.readInt());
    }
}
