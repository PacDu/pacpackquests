package fr.pacdu.pacpackquests.reward;

import fr.pacdu.pacpackquests.RewardType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public sealed interface QuestReward permits ItemReward, XpReward, LevelReward, CommandReward {

    RewardType type();

    int amount();

    void grant(ServerPlayer player, RegistryAccess registryManager);

    ItemStack createIconStack(HolderLookup.Provider registries);

    void write(FriendlyByteBuf buf);

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
