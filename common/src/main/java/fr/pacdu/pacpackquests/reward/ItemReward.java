package fr.pacdu.pacpackquests.reward;

import fr.pacdu.pacpackquests.RewardType;
import fr.pacdu.pacpackquests.util.ItemUtils;
import fr.pacdu.pacpackquests.util.TagUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ItemReward(String target, int amount) implements QuestReward {

    @Override
    public RewardType type() {
        return RewardType.ITEM;
    }

    @Override
    public void grant(ServerPlayer player, RegistryAccess registryManager) {
        if (target == null || target.isEmpty() || amount <= 0) return;

        // Random reward from a tag (e.g. "#minecraft:logs")
        if (target.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(target.substring(1));
            if (tagId != null) {
                List<Item> items = TagUtils.getAllItemsInTag(tagId);
                if (!items.isEmpty()) {
                    Item randomItem = items.get(player.getRandom().nextInt(items.size()));
                    giveItemStack(player, new ItemStack(randomItem, amount));
                    return;
                }
            }
        }

        // Item with components or standard item
        ItemStack rewardStack = ItemUtils.fromCommandString(target, registryManager);
        if (!rewardStack.isEmpty()) {
            rewardStack.setCount(amount);
            giveItemStack(player, rewardStack);
        }
    }

    private void giveItemStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public ItemStack createIconStack(HolderLookup.Provider registries) {
        if (target == null || target.isEmpty()) return ItemStack.EMPTY;
        return ItemUtils.fromCommandString(target, registries);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(RewardType.ITEM);
        buf.writeUtf(target != null ? target : "");
        buf.writeInt(amount);
    }

    public static ItemReward read(FriendlyByteBuf buf) {
        return new ItemReward(buf.readUtf(), buf.readInt());
    }
}
