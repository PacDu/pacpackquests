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

/**
 * A quest reward that grants item stacks (direct items, items with NBT/components, or random items from a tag).
 *
 * @param target The item identifier string, SNBT command string, or item tag (prefixed with #).
 * @param amount The number of items to grant.
 */
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
        if (target != null && !target.isEmpty()) {
            if (target.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(target.substring(1));
                if (tagId != null) {
                    List<Item> items = TagUtils.getAllItemsInTag(tagId);
                    if (!items.isEmpty()) {
                        long cycle = System.currentTimeMillis() / 1500;
                        int index = (int) (cycle % items.size());
                        return new ItemStack(items.get(index), amount);
                    }
                }
            } else {
                return ItemUtils.fromCommandString(target, registries);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(RewardType.ITEM);
        buf.writeUtf(this.target != null ? this.target : "");
        buf.writeInt(this.amount);
    }

    /**
     * Reads an {@link ItemReward} from the network buffer.
     *
     * @param buf The buffer to read from.
     * @return The read {@link ItemReward}.
     */
    public static ItemReward read(FriendlyByteBuf buf) {
        return new ItemReward(buf.readUtf(), buf.readInt());
    }
}
